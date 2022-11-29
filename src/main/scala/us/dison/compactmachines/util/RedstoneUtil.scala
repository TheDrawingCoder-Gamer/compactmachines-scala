package us.dison.compactmachines.util

import net.minecraft.world.World
import net.minecraft.util.math.BlockPos
import net.minecraft.block.Blocks 
import net.minecraft.block.BlockState 
import net.minecraft.block.RedstoneWireBlock 
import net.minecraft.util.math.Direction 
import us.dison.compactmachines.util.canequal._

object RedstoneUtil {
  def getPower(world: World, pos: BlockPos): Int =
    val sanePower = world.getReceivedRedstonePower(pos)
    if sanePower >= 15 then
      sanePower
    else
      var insanePower = 0
      for (dir <- Direction.values.nn.filter(_ != Direction.DOWN).map(_.nn)) {
        val state = world.getBlockState(pos.offset(dir)).nn
        if (state.isOf(Blocks.REDSTONE_WIRE)) {
          val wirePower = state.get(RedstoneWireBlock.POWER).nn
          if wirePower >= 15 then return wirePower
          if wirePower > insanePower then insanePower = wirePower
        }
      }
      Math.max(sanePower, insanePower)

  def getDirectionalPower(world: World, pos: BlockPos, direction: Direction): Int =
    val newPos = pos.offset(direction).nn
    val power = world.getEmittedRedstonePower(newPos, direction).nn
    if power >= 15 then
      power
    else
      val state = world.getBlockState(newPos).nn
      Math.max(power, if state.isOf(Blocks.REDSTONE_WIRE) then Integer2int(state.get(RedstoneWireBlock.POWER).nn) else 0)
}
        


     
