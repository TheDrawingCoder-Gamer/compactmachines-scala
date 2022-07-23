package us.dison.compactmachines.block

import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import us.dison.compactmachines.CompactMachines;
import us.dison.compactmachines.block.entity.MachineWallBlockEntity;
import us.dison.compactmachines.block.entity.TunnelWallBlockEntity;
import us.dison.compactmachines.enums.TunnelDirection;
import us.dison.compactmachines.data.persistent.Room;
import us.dison.compactmachines.data.persistent.RoomManager;
import us.dison.compactmachines.data.persistent.tunnel.Tunnel;
import us.dison.compactmachines.data.persistent.tunnel.TunnelType;
import us.dison.compactmachines.item.PSDItem;
import us.dison.compactmachines.item.TunnelItem;
import us.dison.compactmachines.util.TunnelUtil;
import net.minecraft.loot.context.LootContext.Builder
import java.{util => ju}

abstract class AbstractWallBlock(settings: AbstractBlock.Settings, private val breakable: Boolean) extends BlockWithEntity(settings): 
  @annotation.nowarn("deprecation")
  override def onUse(state:BlockState | Null, world: World | Null, pos:BlockPos | Null, player:PlayerEntity | Null, hand:Hand | Null, hit:BlockHitResult | Null): ActionResult | Null =
    val s = super.onUse(state, world, pos, player, hand, hit)
    
    if world != CompactMachines.cmWorld then 
      ActionResult.FAIL 
    else 
      world.getBlockEntity(pos).nn match 
        case _ : TunnelWallBlockEntity => ActionResult.FAIL 
        case wall : MachineWallBlockEntity => 
          val roomManager = CompactMachines.roomManager 
          roomManager.getRoomByNumber(wall.parentID.getOrElse(-1)) match 
            case None => ActionResult.FAIL
            case Some(room) =>
              player.getStackInHand(hand).nn.getItem().nn match 
                case _ : TunnelItem => 
                  val stackNbt = player.getStackInHand(hand).getNbt()
                  TunnelUtil.typeFromNbt(stackNbt) match 
                    case Some(tunnelType) =>
                      world.breakBlock(pos, false) 
                      world.setBlockState(pos, 
                        CompactMachines.BLOCK_WALL_TUNNEL.getDefaultState,
                        Block.NOTIFY_ALL | Block.FORCE_STATE
                      )
                      world.getBlockEntity(pos) match 
                        case tunnelEntity : TunnelWallBlockEntity => 
                          val tunnel = Tunnel(wall.getPos(), TunnelDirection.NoDir, tunnelType, false, false, false, false)
                          roomManager.addTunnel(room.number, tunnel)
                          wall.parentID match 
                            case Some(id) => 
                              tunnelEntity.setParentID(id)
                          tunnelEntity.setTunnelType(tunnelType) 
                          tunnelEntity.setConnectedToItem(false) 
                          tunnelEntity.setConnectedToFluid(false) 
                          tunnelEntity.setConnectedToEnergy(false) 
                          world.setBlockState(pos, world.getBlockState(pos),
                            Block.NOTIFY_ALL | Block.FORCE_STATE)
                          world.nn.getBlockState(pos).getBlock().nn match 
                            case tunnelWall : TunnelWallBlock => 
                              tunnelWall.setTunnel(tunnel) 
                case _ => 

                  if (world.isClient) return ActionResult.SUCCESS 
                  if (world.getServer() == null) return ActionResult.FAIL 

                  val serverPlayer = player.asInstanceOf[ServerPlayerEntity] 
                  val machineWorld = world.getServer().getWorld(RegistryKey.of(Registry.WORLD_KEY, room.world));
                  val machinePos = room.machine

                  if player.getStackInHand(hand).nn.getItem().isInstanceOf[PSDItem] then 
                    CompactMachines.LOGGER.info("Teleporting player " + player.getDisplayName.asString + "out of machine #" + room.number.toString + " at: " + room.center.toShortString)
                    serverPlayer.teleport(machineWorld, machinePos.getX(), machinePos.getY(), machinePos.getZ(), 0, 0)
                    roomManager.rmPlayer(room.number, serverPlayer.getUuidAsString)
                    return ActionResult.SUCCESS
      s
  @annotation.nowarn("deprecation") 
  override def calcBlockBreakingDelta(state: BlockState | Null, player: PlayerEntity | Null, world: BlockView | Null, pos: BlockPos | Null): Float = 
    if this.breakable then 
      super.calcBlockBreakingDelta(state, player, world, pos)
    else 
      0
  override def getHardness(): Float = 
    if this.breakable then super.getHardness() else -1

  override def getBlastResistance(): Float = 
    if this.breakable then super.getBlastResistance() else 3600000

  @annotation.nowarn("deprecation") 
  override def getDroppedStacks(state: BlockState | Null, builder: Builder | Null): ju.List[ItemStack] | Null = 
    if this.breakable then 
      super.getDroppedStacks(state, builder)
    else 
      ju.List.of(ItemStack.EMPTY)
  override def getRenderType(state: BlockState | Null): BlockRenderType | Null = 
    BlockRenderType.MODEL
  override def getTicker[T <: BlockEntity](world: World | Null, state: BlockState | Null, kind: BlockEntityType[T] | Null): BlockEntityTicker[T] | Null
  override def createBlockEntity(pos: BlockPos | Null, state: BlockState | Null) : BlockEntity | Null
