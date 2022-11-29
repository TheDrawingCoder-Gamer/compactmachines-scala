package us.dison.compactmachines.block.entity 

import net.minecraft.block.BlockState 
import net.minecraft.util.math.BlockPos 
import net.minecraft.world.World 
import us.dison.compactmachines.CompactMachines
import net.minecraft.block.entity.BlockEntity

class MachineWallBlockEntity(pos: BlockPos, state: BlockState) extends 
  BlockEntity(CompactMachines.MACHINE_WALL_BLOCK_ENTITY : @unchecked, pos, state) with AbstractWallBlockEntity

object MachineWallBlockEntity: 
  def tick(world: World, blockPos: BlockPos, blockState: BlockState, wallBlockEntity: MachineWallBlockEntity): Unit = {}
