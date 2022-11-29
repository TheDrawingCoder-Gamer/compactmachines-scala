package us.dison.compactmachines.util

import net.minecraft.block.entity.BlockEntity
import net.minecraft.world.World
import net.minecraft.util.math.BlockPos
import net.minecraft.block.BlockState
trait Ticker[T <: BlockEntity] {
  def tick(world: World, pos: BlockPos, state: BlockState, entity: T): Unit
}
