package us.dison.compactmachines.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.client.render.RenderLayer
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.registry.Registry
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.block.TunnelWallBlock
import us.dison.compactmachines.block.entity.TunnelWallBlockEntity
import us.dison.compactmachines.block.entity.TunnelRenderAttachmentData
import us.dison.compactmachines.enums.MachineSize
import us.dison.compactmachines.data.persistent.tunnel.TunnelType

import java.util.UUID
import scala.language.unsafeNulls
import us.dison.compactmachines.CompactMachines.*
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.StartWorldTick
import net.minecraft.client.world.ClientWorld
import us.dison.compactmachines.crafting.projector.HologramRenderer
import net.fabricmc.fabric.api.renderer.v1.Renderer
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AfterEntities
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.DebugRender
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AfterTranslucent
@Environment(EnvType.CLIENT)
object CompactMachinesClient extends ClientModInitializer {
  override def onInitializeClient(): Unit = {
    // REGISTER machine tooltip callback
    ItemTooltipCallback.EVENT.nn.register((stack, context, lines) => {
      if (Registry.ITEM.nn.getId(stack.nn.getItem()).toString().startsWith(s"$MOD_ID:machine_")) { // Machine number tooltip
        val pathParts = Registry.ITEM.nn.getId(stack.nn.getItem()).getPath().nn.split("_")
        if pathParts.length > 2 then
          MachineSize.getFromSize(pathParts(1)) match

            case Some(machineSize) =>
              lines.add(1, TranslatableText("tooltip.compactmachines.machine.size", machineSize.size, machineSize.size, machineSize.size).formatted(Formatting.GRAY))
              if (stack.getNbt() != null) {
                val owner = stack.getSubNbt("BlockEntityTag").getUuid("uuid")
                var playerName = owner.toString()
                val handler = MinecraftClient.getInstance().getNetworkHandler
                if (handler != null) {
                  val playerListEntry = handler.getPlayerListEntry(owner)
                  if (playerListEntry != null) {
                    playerName = playerListEntry.getProfile().getName()
                  }
                }
                if (context.isAdvanced()) lines.add(1, TranslatableText("tooltip.compactmachines.machine.owner", playerName).formatted(Formatting.GRAY))
                lines.add(1, TranslatableText("tooltip.compactmachines.machine.id", stack.getSubNbt("BlockEntityTag").getInt("number")).formatted(Formatting.GRAY))
              }
            case _ => ()
      } else if (Registry.ITEM.getId(stack.getItem()).equals(ID_WALL_UNBREAKABLE)) { // Unbreakable wall tooltip
        lines.add(1, TranslatableText("tooltip.compactmachines.details.solid_wall").formatted(Formatting.RED))
      }
    })

    // Make Tunnel Wall Blocks have transparent render layers
    BlockRenderLayerMap.INSTANCE.putBlock(CompactMachines.BLOCK_WALL_TUNNEL, RenderLayer.getCutout)
    // BlockRenderLayerMap.INSTANCE.putBlock(CompactMachines.BLOCK_PROJECTOR, RenderLayer.getTranslucent)
    // Tint Tunnel Wall Blocks
    ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) =>
      world.getBlockEntity(pos) match {
        case tunnelWall: TunnelWallBlockEntity =>
          val ra = tunnelWall.getRenderAttachmentData().asInstanceOf[TunnelRenderAttachmentData]
          val daType = ra.tunnelType
          val isConnected = ra.isConnected
          tintIndex match {
            case 0 => daType match {
              case None => 0xff00ff
              case Some(t) => t.color
            }
            case 1 => daType match {
              case None => 0xff00ff
              case Some(TunnelType.Redstone) => if tunnelWall.outgoing then 0xeb3434 else 0x34eb6e
              case Some(TunnelType.Normal) => if isConnected then 0xaaaabb else 0x222233
            }
            case _ => 0xff00ff
          }
        case _ => 0xff00ff
      }

      , BLOCK_WALL_TUNNEL)
    ClientTickEvents.START_WORLD_TICK.register((world: ClientWorld) => {
      HologramRenderer.tick()
    })
    WorldRenderEvents.AFTER_TRANSLUCENT.register((x: WorldRenderContext) => {
      HologramRenderer.render(x.matrixStack())
    })

  }
}