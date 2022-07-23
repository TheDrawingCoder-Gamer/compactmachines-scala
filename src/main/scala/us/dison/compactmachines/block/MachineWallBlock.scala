package us.dison.compactmachines.block 

import net.minecraft.block._
import net.minecraft.block.entity.BlockEntity 
import net.minecraft.block.entity.BlockEntityTicker 
import net.minecraft.block.entity.BlockEntityType 
import net.minecraft.util.math.BlockPos 
import net.minecraft.world.World 
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.block.entity.MachineWallBlockEntity 
import us.dison.compactmachines.block.AbstractWallBlock

class MachineWallBlock(settings: AbstractBlock.Settings, breakable: Boolean) extends AbstractWallBlock(settings, breakable): 
  override def getTicker[T <: BlockEntity](world: World | Null, state: BlockState | Null, entityType: BlockEntityType[T] | Null) : BlockEntityTicker[T] | Null = 
    BlockWithEntity.checkType(entityType, CompactMachines.MACHINE_WALL_BLOCK_ENTITY, MachineWallBlockEntity.tick)
  override def createBlockEntity(pos: BlockPos | Null, state: BlockState | Null) : BlockEntity | Null = 
    MachineWallBlockEntity(pos, state)
