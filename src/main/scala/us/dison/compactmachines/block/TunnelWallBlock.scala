package us.dison.compactmachines.block

import net.minecraft.block.Block;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtString;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import us.dison.compactmachines.CompactMachines;
import us.dison.compactmachines.block.entity.MachineBlockEntity;
import us.dison.compactmachines.block.entity.MachineWallBlockEntity;
import us.dison.compactmachines.block.entity.TunnelWallBlockEntity;
import us.dison.compactmachines.enums.TunnelDirection;
import us.dison.compactmachines.data.persistent.Room;
import us.dison.compactmachines.data.persistent.RoomManager;
import us.dison.compactmachines.data.persistent.tunnel.Tunnel;
import us.dison.compactmachines.data.persistent.tunnel.TunnelType;
import us.dison.compactmachines.util.RedstoneUtil;
import us.dison.compactmachines.util.TunnelUtil;
import us.dison.compactmachines.util.HitUtil;
import net.minecraft.block.BlockWithEntity

class TunnelWallBlock(settings: net.minecraft.block.AbstractBlock.Settings, breakable: Boolean) extends AbstractWallBlock(settings, breakable):
    setDefaultState(getStateManager().getDefaultState().`with`(TunnelWallBlock.CONNECTED_SIDE, TunnelDirection.NoDir))

    private var tunnelVar: Option[Tunnel] = Option.empty 
    override def neighborUpdate(state: BlockState, world: World, pos: BlockPos, block: Block, fromPos: BlockPos, notify: Boolean): Unit = 
      if (block.equals(this)) then 
        () 
      else 
        if notify then 
          update(state, world, pos) 
    protected def update(state: BlockState, world: World, pos: BlockPos): Unit = 
      world.getBlockEntity(pos) match 
        case wall : TunnelWallBlockEntity => 
          wall.getMachineEntity() match 
            case Some(machineEntity) => 
              machineEntity.getWorld() match 
                case null => 
                  CompactMachines.LOGGER.warn("Machine entity world is null")
                case machineWorld => 
                  world.updateNeighbors(pos, this) 
                  machineWorld.updateNeighborsAlways(machineEntity.getPos(), machineWorld.getBlockState(machineEntity.getPos()).getBlock())
            case None => 
              CompactMachines.LOGGER.warn("Machine entity is null")
    override protected def appendProperties(builder: StateManager.Builder[Block, BlockState]): Unit = 
      builder.add(TunnelWallBlock.CONNECTED_SIDE) 
    override def getTicker[T <: BlockEntity](world: World, state: BlockState, kind: BlockEntityType[T]): BlockEntityTicker[T] | Null = 
      BlockWithEntity.checkType(kind, CompactMachines.TUNNEL_WALL_BLOCK_ENTITY, TunnelWallBlockEntity.tick)
    override def createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity | Null = 
      TunnelWallBlockEntity(pos, state)
    def setTunnel(tunnel: Tunnel) = 
      this.tunnelVar = Option(tunnel) 
    def tunnel = tunnelVar
    override def emitsRedstonePower(state: BlockState) = true
    override def getWeakRedstonePower(state: BlockState, world: BlockView, pos: BlockPos, dir: Direction): Int = 
     world.getBlockEntity(pos) match 
      case wall: TunnelWallBlockEntity => 
        if wall.tunnelType == TunnelType.Redstone && state.get(TunnelWallBlock.CONNECTED_SIDE) != TunnelDirection.NoDir && !wall.outgoing then 
          wall.getMachineEntity() match 
            case None => 
              CompactMachines.LOGGER.warn("Machine entity is null")
              0
            case Some(machineEntity) => 
              machineEntity.getWorld() match 
                case null => 
                  CompactMachines.LOGGER.warn("Machine world is null") 
                  0
                case machineWorld => 
                  val connectedSide = state.get(TunnelWallBlock.CONNECTED_SIDE).toDirection()
                  return RedstoneUtil.getDirectionalPower(machineWorld, machineEntity.getPos(), connectedSide.orNull)
        else 
          0
    override def onUse(blockState: BlockState, world:World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult):  ActionResult = 
      world.getBlockEntity(pos) match 
        case wall: TunnelWallBlockEntity =>
          if player.getStackInHand(hand).equals(ItemStack.EMPTY) then 
            if world.isClient() then 
              ActionResult.SUCCESS 
            else 
              if world.getRegistryKey eq CompactMachines.CMWORLD_KEY then 
                val roomManager = CompactMachines.roomManager 
                wall.parentID.flatMap(roomManager.getRoomByNumber(_)) match
                  case None => ActionResult.PASS
                  case Some(room) => 
                    world.setBlockState(pos, CompactMachines.BLOCK_WALL_UNBREAKABLE.getDefaultState())
                    // wall.setParentID(wall.parentID) // wtf is this 
                    TunnelUtil.fromRoomAndPos(room, pos) match 
                      case None => 
                        ActionResult.FAIL 
                      case Some(t) => 
                        val state = world.getBlockState(pos).nn 
                        val switchingCircutDir = 
                          if t.tunnelType == TunnelType.Redstone then 
                            val (hitX, hitY) = HitUtil.getFaceHit(hit, pos) 
                            if hitX >= 0.69 && hitX <= 0.94 && hitY >= 0.69 && hitY <= 0.94 then 
                              true 
                            else 
                              false 
                          else 
                            false
                        if switchingCircutDir then 
                          wall.setOutgoing(!wall.outgoing)
                          this.update(state, world, pos) 
                          player.sendMessage(TranslatableText("compactmachines.iodirection.going",
                          TranslatableText("compactmachines.iodirection." + (if wall.outgoing then "outgoing" else "incoming"))), true)
                          ActionResult.SUCCESS
                        else 
                          if hand == Hand.MAIN_HAND then 
                            state.getBlock() match 
                              case block : TunnelWallBlock => 
                                // why tf does the original code constantly refetch the block entity 
                                wall.tunnel match 
                                  case Some(oldTunnel) => 
                                    val nextSide = TunnelUtil.nextSide(room, state.get(TunnelWallBlock.CONNECTED_SIDE))
                                    world.setBlockState(pos, state.`with`(TunnelWallBlock.CONNECTED_SIDE, nextSide))
                                    val newTunnel = oldTunnel.copy(face = nextSide) 
                                    block.setTunnel(newTunnel) // what 
                                    roomManager.updateTunnel(room.number, newTunnel)
                                    player.sendMessage(TranslatableText("compactmachines.direction.side", 
                                      TranslatableText("compactmachines.direction." + nextSide.direction.toLowerCase())), true)
                                    ActionResult.SUCCESS 
                                  case _ => 
                                    ActionResult.SUCCESS
                              case _ => ActionResult.FAIL
                          else ActionResult.FAIL
                      else
                          ActionResult.SUCCESS

                                
                        
              else
                ActionResult.PASS
                  
          
object TunnelWallBlock: 
  val CONNECTED_SIDE: EnumProperty[TunnelDirection] = EnumProperty.of("connected_side", classOf[TunnelDirection])
