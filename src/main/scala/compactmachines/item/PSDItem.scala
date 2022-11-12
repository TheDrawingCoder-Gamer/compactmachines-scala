package us.dison.compactmachines.item

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import us.dison.compactmachines.CompactMachines;
import us.dison.compactmachines.block.MachineBlock;
import us.dison.compactmachines.block.entity.MachineWallBlockEntity;
import us.dison.compactmachines.data.persistent.Room;
import us.dison.compactmachines.data.persistent.RoomManager;
import us.dison.compactmachines.gui.PSDScreen;
import net.minecraft.util.registry.Registry
import net.minecraft.util.registry.RegistryKey
import us.dison.compactmachines.util.RoomUtil
import net.minecraft.server.MinecraftServer

class PSDItem(settings: Item.Settings) extends Item(settings):
  private def teleportOutOfRoom(server: MinecraftServer, player: ServerPlayerEntity) : Unit = 
    CompactMachines.roomManager.getRoomFromPos(player.getBlockPos()).foreach(room => 
      val machineWorld = server.getWorld(RegistryKey.of(Registry.WORLD_KEY, room.world))
      val machinePos = room.machine 
      CompactMachines.LOGGER.info("Teleporting player " + player.getDisplayName.asString + " out of machine #" + room.number.toString + " at: " + room.center.toShortString)
      RoomUtil.teleportOutOfRoom(machineWorld, player, room) 
      // CompactMachines.roomManager.rmPlayer(room.number, player.getUuid)
    )
  override def use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult[ItemStack] | Null = 
    super.use(world, user, hand)
    if world.isClient() then 
      if user.isSneaking() then return TypedActionResult.pass(user.getStackInHand(hand)) 
      if world.getRegistryKey().equals(CompactMachines.CMWORLD_KEY) then 
        () 
      else 
        MinecraftClient.getInstance().nn.setScreen(PSDScreen(TranslatableText("compactmachines.psd.pages.machine.title")))
      TypedActionResult.success(user.getStackInHand(hand))
    else 
      Option(world.getServer()).map(server => 
          val serverPlayer = user.asInstanceOf[ServerPlayerEntity]
          if (user.isSneaking()) {
            CompactMachines.roomManager.getRoomFromPos(user.getBlockPos()).foreach(room => 
              CompactMachines.roomManager.updateSpawnPos(room.number, user.getBlockPos())
              serverPlayer.sendMessage(TranslatableText("message.compactmachines.spawnpoint_set"), true)
            )
          } else {
            teleportOutOfRoom(server, serverPlayer)
          }
          TypedActionResult.success(user.getStackInHand(hand))
      ).getOrElse(TypedActionResult.pass(user.getStackInHand(hand)))
  
  
  override def useOnBlock(context: ItemUsageContext): ActionResult =
    super.useOnBlock(context) 
    // sanity drainage 
    if (context.getWorld().getBlockState(context.getBlockPos()).getBlock().isInstanceOf[MachineBlock]) {
      ActionResult.SUCCESS 
    } else {
      use(context.getWorld(), context.getPlayer(), context.getHand()).getResult()
    }
