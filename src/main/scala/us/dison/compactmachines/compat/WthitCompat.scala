package us.dison.compactmachines.compat

import mcp.mobius.waila.api.{
  IWailaPlugin, 
  IRegistrar, 
  ITooltipComponent, 
  IBlockComponentProvider, 
  IServerDataProvider, 
  ITooltip,
  IBlockAccessor,
  IServerAccessor,
  IPluginConfig,
  TooltipPosition} 
import net.minecraft.text.{Text, TranslatableText}
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.MinecraftClient
import us.dison.compactmachines.data.persistent.{Room, RoomManager}
import us.dison.compactmachines.data.persistent.tunnel.TunnelType
import us.dison.compactmachines.enums.TunnelDirection
import us.dison.compactmachines.block.entity.MachineBlockEntity
import us.dison.compactmachines.block.MachineBlock
import net.minecraft.nbt.{NbtCompound, NbtList}
import us.dison.compactmachines.CompactMachines
import net.minecraft.util.Identifier

class CompactWthitPlugin extends IWailaPlugin {
  override def register(registrar : IRegistrar): Unit = {
    registrar.addConfig(CompactWthitConfig.machineBlock, true)
    CompactMachines.LOGGER.info("registering plugin...")
    registrar.addComponent(MachineBlockTooltip, TooltipPosition.BODY, classOf[MachineBlockEntity])
    registrar.addBlockData[MachineBlockEntity, MachineBlockEntity](MachineBlockTooltip.asInstanceOf[IServerDataProvider[MachineBlockEntity]], classOf[MachineBlockEntity])
    CompactMachines.LOGGER.info("registered!")
  }
}


object MachineTooltip {
  val textOffset: Int = MinecraftClient.getInstance().textRenderer.fontHeight + 5
}
/*
class MachineTooltip(val room : Room, val x : Int, val y : Int) extends ITooltipComponent {
    private def CLIENT = MinecraftClient.getInstance()
    override def render(stack: MatrixStack, mx: Int, my: Int, delta: Float): Unit = {
      stack.push()
      var ypos = 0 
      for (value <- TunnelDirection.values) {
        if (value != TunnelDirection.NoDir) {
          room.tunnels.find(it => it.face == value).foreach { it => 
            CLIENT.textRenderer.draw(stack, text, x.toFloat, ypos.toFloat, 0xffffff)
            ypos = ypos + MachineTooltip.textOffset
          } 
        }
      } 
    }
    override def getHeight() = room.tunnels.filter(it => it.face != TunnelDirection.NoDir).length * MachineTooltip.textOffset
    override def getWidth() = 64 


}
*/
object MachineBlockTooltip extends IBlockComponentProvider with IServerDataProvider[MachineBlockEntity] {
  override def appendBody(tooltip : ITooltip, accessor: IBlockAccessor, conf : IPluginConfig): Unit = {
    if (conf.getBoolean(CompactWthitConfig.machineBlock)) {
      val data = accessor.getServerData
      val list = data.getList("tunnels", 10)
      for (i <- 0 until list.size()) {
        val item = list.getCompound(i) 
        val dir = TunnelDirection.fromOrdinal(item.getInt("tdir"))
        val ttype = TunnelType.fromOrdinal(item.getInt("ttype"))
        val text = TranslatableText(s"compactmachines.direction.${dir.asString()}")
           .append(": ")
           .append(TranslatableText(s"item.compactmachines.tunnels.${ttype.asString()}"))
        tooltip.addLine(text)
      }
    }
 
  }
  override def appendServerData(data : NbtCompound, accessor : IServerAccessor[MachineBlockEntity], conf : IPluginConfig): Unit = {
    CompactMachines.roomManager.getRoomByNumber(accessor.getTarget.machineID.get).foreach { it => 
      val list = NbtList()
      for (tunnel <- it.tunnels.filter(_.face != TunnelDirection.NoDir)) {
        val compound = NbtCompound()
        compound.putInt("tdir", tunnel.face.ordinal())
        compound.putInt("ttype", tunnel.tunnelType.ordinal())
        list.add(compound)
      }
      data.put("tunnels", list)
    }
  }
}

object CompactWthitConfig {
  private def rl(rl : String) = Identifier(CompactMachines.MOD_ID, rl)
  val general: Identifier = rl("general")
  val machineBlock: Identifier = rl("machineblock.enabled")
}
