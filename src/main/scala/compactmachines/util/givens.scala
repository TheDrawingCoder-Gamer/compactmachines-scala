package us.dison.compactmachines.util

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction 
import net.minecraft.block.BlockState
import Direction.Axis
import net.minecraft.state.property.Property
extension (axis : Axis) {
  def crossAxis = axis match {
    case Axis.X => Axis.Z 
    case Axis.Z => Axis.X 
    case Axis.Y => Axis.Y
  } 
}
extension (state : BlockState) {
  def copy[T <: Comparable[T]](prop : Property[T], value : T) = {
    state.`with`(prop, value)
  }
}
