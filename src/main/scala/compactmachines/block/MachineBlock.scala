package us.dison.compactmachines.block

import net.minecraft.block.* 
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import us.dison.compactmachines.CompactMachines;
import us.dison.compactmachines.enums.{MachineSize, TunnelDirection};
import us.dison.compactmachines.block.entity.MachineBlockEntity;
import us.dison.compactmachines.block.entity.TunnelWallBlockEntity;
import us.dison.compactmachines.data.persistent.Room;
import us.dison.compactmachines.data.persistent.RoomManager;
import us.dison.compactmachines.data.persistent.tunnel.Tunnel;
import us.dison.compactmachines.data.persistent.tunnel.TunnelType
import us.dison.compactmachines.item.PSDItem;
import us.dison.compactmachines.util.RedstoneUtil;
import us.dison.compactmachines.util.RoomUtil;
import us.dison.compactmachines.util.TunnelUtil
import net.minecraft.util.registry.Registry
import net.minecraft.util.math.Direction.Axis
class MachineBlock(settings: AbstractBlock.Settings, val machineSize: Option[MachineSize]) extends BlockWithEntity(settings): 
  
  private var lastInsidePlayerWarning = 0

  @annotation.nowarn("cat=deprecation") 
  override def onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult =
    super.onUse(state, world, pos, player, hand, hit) 
    if !world.isClient() then 
      if world.getBlockEntity(pos).isInstanceOf[MachineBlockEntity] then 
        val machineEntity = world.getBlockEntity(pos).asInstanceOf[MachineBlockEntity]
        player match 
          case serverPlayer : ServerPlayerEntity =>
            serverPlayer.getStackInHand(hand).getItem() match 
              case _ : PSDItem => 
                val roomManager = CompactMachines.roomManager
                val cmWorld = world.getServer().getWorld(CompactMachines.CMWORLD_KEY)
                (machineEntity.machineID, machineSize) match 
                  case (None, Some(mSize)) => 
                    // make new room 
                    val machineID = RoomUtil.nextID(roomManager) 
                    machineEntity.machineID = Some(machineID)
                    machineEntity.owner = Some(serverPlayer.getUuid)
                    val roomCenterPos = RoomUtil.getCenterPosByID(machineID)
                    val spawnPos = roomCenterPos.add(0, -(mSize.size/2d)+1, 0)
                    serverPlayer.sendMessage(Text.translatable("message.compactmachines.generating_room"), true) 
                    machineEntity.size match 
                      case Some(size) => 
                        RoomUtil.generateRoomFromId(cmWorld, machineID, size)
                        serverPlayer.sendMessage(Text.translatable("message.compactmachines.ready").formatted(Formatting.GREEN), true)
                        roomManager.addRoom(Room(world.getRegistryKey.getValue, serverPlayer.getUuidAsString, pos, roomCenterPos, spawnPos, machineID, List(), List()))
                        ActionResult.SUCCESS
                      case None => 
                        ActionResult.FAIL
                  case (Some(id), _) => 
                    (roomManager.getRoomByNumber(id) : Option[Room] ) match 
                      case Some(room) => 
                        val spawnPos = room.spawnPos 
                        CompactMachines.LOGGER.info("Teleporting player " + player.getDisplayName().toString() + " into machine #" + id + " at: " + spawnPos.toShortString)
                        serverPlayer.teleport(cmWorld, spawnPos.getX + 0.5d, spawnPos.getY + 1d, spawnPos.getZ + 0.5d, 0, 0)
                        roomManager.addPlayer(id, serverPlayer.getUuid) 
                        ActionResult.SUCCESS 
                      case None => 
                        CompactMachines.LOGGER.error("Player " + player.getDisplayName().toString() + " attempted to enter a machine with an invalid id! (#" + id.toString + ")")
                        player.sendMessage(Text.translatable("message.compactmachines.invalid_room").formatted(Formatting.RED), false)
                        ActionResult.PASS 
                  case _ => 
                    CompactMachines.LOGGER.warn("Room doesn't have assigned size")
                    ActionResult.FAIL 
              case item =>
                CompactMachines.LOGGER.info(item.getRegistryEntry().isIn(CompactMachines.WRENCH_TAG))
                if (item.getRegistryEntry().isIn(CompactMachines.WRENCH_TAG)) {
                  CompactMachines.LOGGER.info("using wrench")
                  val axis = hit.getSide().getAxis()
                  val room = machineEntity.machineID.map(it => CompactMachines.roomManager.getRoomByNumber(it).get)
                  val cmWorld = world.getServer().getWorld(CompactMachines.CMWORLD_KEY)
                  room.foreach { room => 
                    val newTunnels = room.tunnels.map(it => it.copy(face = it.face.rotateClockwise(axis)))
                    for (tunnel <- newTunnels) {
                      cmWorld.getBlockEntity(tunnel.pos) match {
                        case tunnelEntity : TunnelWallBlockEntity => 
                          tunnelEntity.tunnel = tunnel
                        case _ => ()
                      }
                    }
                  }
                  ActionResult.SUCCESS
                } else ActionResult.FAIL 
      else 
       if (world.getBlockEntity(pos).isInstanceOf[MachineBlockEntity] && (world.getRegistryKey() ne CompactMachines.CMWORLD_KEY)) { 
        val machineEntity = world.getBlockEntity(pos).asInstanceOf[MachineBlockEntity] 
        player match { 
          case clientPlayer : ClientPlayerEntity =>
            val roomManager = CompactMachines.roomManager
            if clientPlayer.getStackInHand(hand).getItem().isInstanceOf[PSDItem] then 
              machineEntity.machineID match 
                case None => 
                  val machineID = RoomUtil.nextID(roomManager) 
                  machineEntity.machineID = Some(machineID)
                  machineEntity.owner = Some(clientPlayer.getUuid)
                case Some(i) => () 
              end match 
              ActionResult.SUCCESS
            else 
              ActionResult.FAIL 
        }
       } else 
        ActionResult.PASS
    else 
      ActionResult.PASS
  override def createBlockEntity(pos: BlockPos, state: BlockState ): BlockEntity = 
    MachineBlockEntity(pos, state, machineSize, None) 
  override def onBreak(world: World, pos: BlockPos | Null, state: BlockState | Null, player: PlayerEntity | Null): Unit = 
    super.onBreak(world, pos, state, player) 
    if world.getBlockEntity(pos).isInstanceOf[MachineBlockEntity] then 
      val blockEntity = world.getBlockEntity(pos).asInstanceOf[MachineBlockEntity] 
      if !world.isClient() then 
        if blockEntity.machineID.isEmpty && player.isCreative then 
          () 
        else 
          val itemStack = this.asItem().nn.getDefaultStack()
          val itemEntity = ItemEntity(world, pos.getX + 0.5, pos.getY, pos.getZ + 0.5, itemStack) 
          blockEntity.setStackNbt(itemStack) 
          world.spawnEntity(itemEntity)
  override def getPickStack(world: BlockView, pos: BlockPos, state: BlockState): ItemStack = 
    val normalStack = super.getPickStack(world, pos, state).nn 
    world.getBlockEntity(pos).getWorld match 
      case clientWorld: ClientWorld =>
        clientWorld.getBlockEntity(pos) match 
          case blockEntity : MachineBlockEntity => 
            blockEntity.machineID match 
              case Some(_) => 
                val itemStack = this.asItem().nn.getDefaultStack() 
                blockEntity.setStackNbt(itemStack)
                itemStack 
              case None => normalStack 
          case _ =>    normalStack 
      case _ => normalStack
  override def onPlaced(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity | Null, itemStack: ItemStack): Unit =
    super.onPlaced(world, pos, state, placer, itemStack) 
     
    world.getBlockEntity(pos) match 
      case blockEntity : MachineBlockEntity => 
        if world.getRegistryKey() eq CompactMachines.CMWORLD_KEY then 
          CompactMachines.roomManager.getRoomFromPos(pos) match 
            case Some(room) => 
              blockEntity.parentID = Some(room.number)
            case None => ()

        blockEntity.owner = Some(placer.nn.getUuid())
        blockEntity.markDirty() 
        if !world.isClient() then 
          blockEntity.machineID.foreach(id => 
            val roomManager = CompactMachines.roomManager 
            roomManager.updateMachinePosAndOwner(id, world.getRegistryKey.getValue, pos, placer.nn.getUuidAsString)
          )
  override def getRenderType(state: BlockState) = BlockRenderType.MODEL 
  override def getTicker[T <: BlockEntity](world: World | Null, state: BlockState | Null, kind: BlockEntityType[T] | Null): BlockEntityTicker[T] | Null = 
    BlockWithEntity.checkType(kind, CompactMachines.MACHINE_BLOCK_ENTITY, MachineBlockEntity.tick) 

  @annotation.nowarn("cat=deprecation") 
  override def calcBlockBreakingDelta(state: BlockState, player: PlayerEntity, world: BlockView, pos: BlockPos): Float = 
    val original = super.calcBlockBreakingDelta(state, player, world, pos)
    world.getBlockEntity(pos) match 
      case blockEntity : MachineBlockEntity => 
        (blockEntity.machineID.map(CompactMachines.roomManager.getRoomByNumber(_)).flatten : Option[Room]) match 
          case Some(room) if (room.players.size > 0) => 
            player match 
              case serverPlayer : ServerPlayerEntity => 
                val now = serverPlayer.server.getTicks() 
                if now >= lastInsidePlayerWarning + 10 then 
                  serverPlayer.sendMessage(Text.translatable("message.compactmachine.player_inside").formatted(Formatting.RED), true)
                  lastInsidePlayerWarning = now
              case _ => ()
            0
          case _ => original 
      case _ => original
  override def emitsRedstonePower(state: BlockState | Null): Boolean = true
  override def getWeakRedstonePower(state: BlockState | Null, world: BlockView | Null, pos: BlockPos, direction: Direction): Int = 
    TunnelUtil.getTunnelOfMachine(world, pos, Option(direction), TunnelType.Redstone).map(tunnel => 
        CompactMachines.LOGGER.info("tunnel exists for " + direction.toString())
        world.getBlockEntity(pos).getWorld().getServer() match 
          case null => 
            CompactMachines.LOGGER.warn("Couldn't fetch server") 
            0
          case server => 
            val cmWorld = server.getWorld(CompactMachines.CMWORLD_KEY) 
            cmWorld.getBlockEntity(tunnel.pos) match 
              case tunnelEntity : TunnelWallBlockEntity if tunnelEntity.outgoing =>
                CompactMachines.LOGGER.info("fetching redstone")
                RedstoneUtil.getPower(cmWorld, tunnel.pos)
              case _ => 0
    ) match
        case Left(why) => 
          CompactMachines.LOGGER.warn(why) 
          0
        case Right(power) => power
              
  override def neighborUpdate(state: BlockState, world: World, pos : BlockPos, block: Block, fromPos: BlockPos, notify: Boolean) : Unit = {
    val roomManager = CompactMachines.roomManager 
    world.getBlockEntity(pos) match {
      case machine : MachineBlockEntity => 
        Option(machine.getWorld().getServer()) match {
          case None => ()
          case Some(server) => 
            machine.machineID.flatMap(roomManager.getRoomByNumber).foreach { room => 
              for (tunnel <- room.tunnels) {
                if (tunnel.face.toDirection().isDefined && tunnel.tunnelType == TunnelType.Redstone) {
                  server.getWorld(CompactMachines.CMWORLD_KEY).updateNeighborsAlways(tunnel.pos, CompactMachines.BLOCK_WALL_TUNNEL)
                }
              }
            }
        }
      case _ => ()

    }
  }
    
// sinful
object MachineBlock 
    


                  
