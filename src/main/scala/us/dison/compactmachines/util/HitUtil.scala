package us.dison.compactmachines.util

import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.BlockPos
import net.minecraft.util.hit.BlockHitResult 
import net.minecraft.util.math.Direction 
import us.dison.compactmachines.CompactMachines

object HitUtil {
  def getFaceHit(hit: BlockHitResult, pos: BlockPos): (Double, Double) = {
    val hitPos = hit.getPos.nn
    val side = hit.getSide.nn
    val hitX = hitPos.getX - pos.getX
    val hitY = hitPos.getY - pos.getY
    val hitZ = hitPos.getZ - pos.getZ
    val res = side match
      case Direction.EAST =>
        (1 - hitZ, hitY)
      case Direction.WEST =>
        (hitZ, hitY)
      case Direction.SOUTH =>
        (hitX, hitY)
      case Direction.NORTH =>
        (1 - hitX, hitY)
      case Direction.UP =>
        (hitX, 1 - hitZ)
      case Direction.DOWN =>
        (hitX, hitZ)
    CompactMachines.LOGGER.info(s"hit at ${res.toString}")
    res
  }
}
