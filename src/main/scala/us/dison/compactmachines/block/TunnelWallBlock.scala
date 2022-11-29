package us.dison.compactmachines.block

import net.minecraft.block.Block
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtString
import net.minecraft.state.StateManager
import net.minecraft.state.property.EnumProperty
import net.minecraft.text.TranslatableText
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.block.BlockWithEntity

import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.block.entity.MachineBlockEntity
import us.dison.compactmachines.block.entity.MachineWallBlockEntity
import us.dison.compactmachines.block.entity.TunnelWallBlockEntity
import us.dison.compactmachines.enums.{TunnelDirection, TunnelGoing}
import us.dison.compactmachines.data.persistent.Room
import us.dison.compactmachines.data.persistent.RoomManager
import us.dison.compactmachines.data.persistent.tunnel.Tunnel
import us.dison.compactmachines.data.persistent.tunnel.TunnelType
import us.dison.compactmachines.util.RedstoneUtil
import us.dison.compactmachines.util.TunnelUtil
import us.dison.compactmachines.util.HitUtil

class TunnelWallBlock(settings: net.minecraft.block.AbstractBlock.Settings, breakable: Boolean) extends BlockWithEntity(settings) with AbstractWallBlock(breakable):
    setDefaultState(getStateManager.getDefaultState
      .`with`(TunnelWallBlock.CONNECTED_SIDE, TunnelDirection.NoDir)
      .`with`(TunnelWallBlock.GOING, TunnelGoing.Neither)
      .`with`(TunnelWallBlock.TUNNEL_TYPE, TunnelType.Normal))

    //noinspection ScalaDeprecation
    @SuppressWarnings("deprecation")
    override def neighborUpdate(state: BlockState, world: World, pos: BlockPos, block: Block, fromPos: BlockPos, notify: Boolean): Unit =
      if block.equals(this) then
        () 
      else 
        if notify then 
          update(state, world, pos) 
    protected def update(state: BlockState, world: World, pos: BlockPos): Unit = 
      world.getBlockEntity(pos) match 
        case wall : TunnelWallBlockEntity => 
          wall.getMachineEntity match
            case Some(machineEntity) => 
              machineEntity.getWorld match
                case null => 
                  CompactMachines.LOGGER.warn("Machine entity world is null")
                case machineWorld => 
                  world.updateNeighbors(pos, this) 
                  machineWorld.updateNeighborsAlways(machineEntity.getPos, machineWorld.getBlockState(machineEntity.getPos).getBlock)
            case None => 
              CompactMachines.LOGGER.warn("Machine entity is null")
    override protected def appendProperties(builder: StateManager.Builder[Block, BlockState]): Unit = 
      builder.add(TunnelWallBlock.CONNECTED_SIDE)
      builder.add(TunnelWallBlock.GOING)
      builder.add(TunnelWallBlock.TUNNEL_TYPE)
    override def getTicker[T <: BlockEntity](world: World, state: BlockState, kind: BlockEntityType[T]): BlockEntityTicker[T] | Null = 
      BlockWithEntity.checkType(kind, CompactMachines.TUNNEL_WALL_BLOCK_ENTITY, TunnelWallBlockEntity.tick)
    override def createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity | Null = 
      TunnelWallBlockEntity(pos, state)
    //noinspection ScalaDeprecation
    @SuppressWarnings("deprecation")
    override def emitsRedstonePower(state: BlockState) = true
    @SuppressWarnings("deprecation")
    override def getWeakRedstonePower(state: BlockState, world: BlockView, pos: BlockPos, dir: Direction): Int = {
     world.getBlockEntity(pos) match
      case wall: TunnelWallBlockEntity => 
        if wall.tunnelType.contains(TunnelType.Redstone) && state.get(TunnelWallBlock.CONNECTED_SIDE) != TunnelDirection.NoDir && !wall.outgoing then 
          CompactMachines.LOGGER.info("getting redstone")
          wall.getMachineEntity match
            case None => 
              CompactMachines.LOGGER.warn("Machine entity is null")
              0
            case Some(machineEntity) => 
              Option(machineEntity.getWorld) match
                case None => 
                  CompactMachines.LOGGER.warn("Machine world is null") 
                  0
                case Some(machineWorld) =>
                  val connectedSide = state.get(TunnelWallBlock.CONNECTED_SIDE).toDirection
                  RedstoneUtil.getDirectionalPower(machineWorld, machineEntity.getPos, connectedSide.orNull)
        else
          CompactMachines.LOGGER.info(wall.outgoing)
          CompactMachines.LOGGER.info(wall.tunnelType.contains(TunnelType.Redstone))
          CompactMachines.LOGGER.info(state.get(TunnelWallBlock.CONNECTED_SIDE).asString())
          0
      case _ => {
        CompactMachines.LOGGER.warn("no valid entity")
        0
      }
    }

  override def onUse(blockState: BlockState, world:World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult):  ActionResult = {
      world.getBlockEntity(pos) match { 
        case wall: TunnelWallBlockEntity =>
          if player.getStackInHand(hand).equals(ItemStack.EMPTY) then 
            if world.isClient then
              ActionResult.SUCCESS 
            else 
              if (world.getRegistryKey eq CompactMachines.CMWORLD_KEY) {  
                val roomManager = CompactMachines.roomManager 
                wall.parentID.flatMap(roomManager.getRoomByNumber) match {
                  case None => ActionResult.PASS
                  case Some(room) => 
                    TunnelUtil.fromRoomAndPos(room, pos) match 
                      case None => 
                        ActionResult.FAIL 
                      case Some(t) => 
                        if (player.isSneaking) {
                          world.setBlockState(pos, CompactMachines.BLOCK_WALL_UNBREAKABLE.getDefaultState)
                          world.getBlockEntity(pos) match {
                            case wallE  : MachineWallBlockEntity => 
                              wallE.parentID = wall.parentID
                              roomManager.rmTunnel(room.number, t) 
                              // spawn item 
                              CompactMachines.LOGGER.info("spawning item") 
                              val itemStack = 
                                if (t.tunnelType == TunnelType.Redstone) { 
                                  CompactMachines.ITEM_REDSTONE_TUNNEL.getDefaultStack().nn 
                                } else {
                                  CompactMachines.ITEM_ITEM_TUNNEL.getDefaultStack().nn 
                                }
                              // wall.tunnelType.foreach((tt : TunnelType) => itemStack.setSubNbt("type", NbtString.of(tt.asString)))
                              val itemPos = pos.offset(hit.getSide, 1)
                              val itemEntity = ItemEntity(world, itemPos.getX.toFloat + 0.5, itemPos.getY.toFloat, itemPos.getZ.toFloat + 0.5, itemStack)
                              world.spawnEntity(itemEntity)
                              ActionResult.SUCCESS

                            case _ => 
                              CompactMachines.LOGGER.warn("MachineWallBlock doesn't have a MachineWallBlockEntity")
                              ActionResult.FAIL 
                          }
                        } else { 
                          // val state = world.getBlockState(pos).nn 
                          val switchingCircutDir = 
                            if t.tunnelType == TunnelType.Redstone then 
                              val (hitX, hitY) = HitUtil.getFaceHit(hit, pos) 
                              if hitX >= 0.69 && hitX <= 0.94 && hitY >= 0.69 && hitY <= 0.94 then 
                                true 
                              else 
                                false 
                            else 
                              false
                          if (switchingCircutDir) { 
                            CompactMachines.roomManager.updateTunnel(room.number, t.copy(outgoing = wall.outgoing))
                            wall.outgoing = !wall.outgoing
                            world.setBlockState(pos, world.getBlockState(pos).`with`(TunnelWallBlock.GOING, if wall.outgoing then TunnelGoing.Outgoing else TunnelGoing.Incoming))
                            this.update(world.getBlockState(pos), world, pos)
                            player.sendMessage(TranslatableText("compactmachines.iodirection.going",
                            TranslatableText(s"compactmachines.iodirection.${if wall.outgoing then "outgoing" else "incoming"}")), true)
                            ActionResult.SUCCESS
                          } else {
                            if hand == Hand.MAIN_HAND then 
                                wall.tunnel match 
                                  case Some(oldTunnel) => 
                                    val nextSide = TunnelUtil.nextSide(room, world.getBlockState(pos).get(TunnelWallBlock.CONNECTED_SIDE))
                                    world.setBlockState(pos, world.getBlockState(pos).`with`(TunnelWallBlock.CONNECTED_SIDE, nextSide))
                                    val newTunnel = oldTunnel.copy(face = nextSide)
                                     
                                    roomManager.updateTunnel(room.number, newTunnel)
                                    CompactMachines.LOGGER.info(wall.tunnel)
                                    player.sendMessage(TranslatableText("compactmachines.direction.side", 
                                      TranslatableText(s"compactmachines.direction.${nextSide.direction.toLowerCase()}")), true)
                                    ActionResult.SUCCESS 
                                  case _ => 
                                    ActionResult.SUCCESS
                            else ActionResult.FAIL
                          }
                        }
                      }
                  } else ActionResult.SUCCESS
              else ActionResult.PASS
        case _ => ActionResult.PASS   
      }
  }

object TunnelWallBlock: 
  val CONNECTED_SIDE: EnumProperty[TunnelDirection] = EnumProperty.of("connected_side", classOf[TunnelDirection])
  val GOING : EnumProperty[TunnelGoing] = EnumProperty.of("going", classOf[TunnelGoing])
  val TUNNEL_TYPE : EnumProperty[TunnelType] = EnumProperty.of("ttype", classOf[TunnelType])
