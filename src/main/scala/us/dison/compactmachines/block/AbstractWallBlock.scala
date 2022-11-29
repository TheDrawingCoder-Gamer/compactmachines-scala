package us.dison.compactmachines.block

import net.minecraft.block.Block
import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContext
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.util.registry.RegistryKey
import net.minecraft.world.BlockView
import net.minecraft.world.World
import org.jetbrains.annotations.Nullable
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.block.entity.MachineWallBlockEntity
import us.dison.compactmachines.block.entity.TunnelWallBlockEntity
import us.dison.compactmachines.enums.TunnelDirection
import us.dison.compactmachines.data.persistent.Room
import us.dison.compactmachines.data.persistent.RoomManager
import us.dison.compactmachines.data.persistent.tunnel.Tunnel
import us.dison.compactmachines.data.persistent.tunnel.TunnelType
import us.dison.compactmachines.item.PSDItem
import us.dison.compactmachines.item.TunnelItem
import us.dison.compactmachines.util.TunnelUtil
import net.minecraft.loot.context.LootContext.Builder
import java.{util => ju}
import us.dison.compactmachines.util.RoomUtil

trait AbstractWallBlock(private val breakable: Boolean) extends Block: 
  
  @annotation.nowarn("cat=deprecation") 
  override def calcBlockBreakingDelta(state: BlockState | Null, player: PlayerEntity | Null, world: BlockView | Null, pos: BlockPos | Null): Float = 
    if this.breakable then 
      super.calcBlockBreakingDelta(state, player, world, pos)
    else 
      0
  override def getHardness: Float =
    if this.breakable then super.getHardness else -1

  override def getBlastResistance: Float =
    if this.breakable then super.getBlastResistance else 3600000

  @annotation.nowarn("cat=deprecation") 
  override def getDroppedStacks(state: BlockState | Null, builder: Builder | Null): ju.List[ItemStack] | Null = 
    if this.breakable then 
      super.getDroppedStacks(state, builder)
    else 
      ju.List.of(ItemStack.EMPTY)
  override def getRenderType(state: BlockState | Null): BlockRenderType | Null = 
    BlockRenderType.MODEL
