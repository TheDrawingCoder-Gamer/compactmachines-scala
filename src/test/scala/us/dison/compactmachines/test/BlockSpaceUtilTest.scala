package us.dison.compactmachines.test

import net.minecraft.util.BlockRotation
import net.minecraft.util.math.BlockPos
import us.dison.compactmachines.util.BlockSpaceUtil
import org.scalatest.funsuite.AnyFunSuite
class BlockSpaceUtilTest extends AnyFunSuite {
  val poses : collection.immutable.Seq[BlockPos] = List(new BlockPos(-1, 0, -1), new BlockPos(1, 0, 1))
  test("BlockSpaceUtil.rotatePositions") {
    val rotated = BlockSpaceUtil.rotatePositions(poses, BlockRotation.CLOCKWISE_90)
    println(rotated)
    assert(rotated(new BlockPos(-1, 0, -1)) == new BlockPos(1, 0, -1))
    assert(rotated(new BlockPos(1, 0, 1)) == new BlockPos(-1, 0, 1))
  }
  test("BlockSpaceUtil.getBoundsForBlocks") {
    val bounds = BlockSpaceUtil.getBoundsForBlocks(poses)
    assert(bounds.getMinX == -1, "Min X == -1")
    assert(bounds.getMinZ == -1, "Min Z == -1")
    assert(bounds.getMaxX == 1, "Max X == 1")
    assert(bounds.getMaxZ == 1, "Max Z == 1")
  }
}
