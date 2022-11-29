package us.dison.compactmachines.util

import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import scala.jdk.CollectionConverters._
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.BlockBox
import scala.jdk.OptionConverters._
import net.minecraft.util.BlockRotation
import us.dison.compactmachines.util.givens.blockPos._

object BlockSpaceUtil {
  def getBlocksIn(box : BlockBox) : Seq[BlockPos] = List.from(BlockPos.stream(box).toList.iterator().asScala).map(_.toImmutable)
  def getInnerPositions(box : BlockBox) : Seq[BlockPos] = {
    val inner = box.shrink(1, 0, 1).offset(1, 0, 1)
    getBlocksIn(inner)
  }
  extension (box : BlockBox) {
    def shrink(x : Int, y : Int, z : Int) : BlockBox = {
      val x1 = box.getMinX - Math.min(0, x)
      val y1 = box.getMinY - Math.min(0, y)
      val z1 = box.getMinZ - Math.min(0, z)
      val x2 = box.getMaxX + Math.max(0, x)
      val y2 = box.getMaxY + Math.max(0, y)
      val z2 = box.getMaxZ + Math.max(0, z)

      BlockBox(x1, y1, z1, x2, y2, z2)
    }
    @annotation.targetName("intersection")
    def &(other : BlockBox) : BlockBox = {
      val x1 = box.getMinX.max(other.getMinX)
      val y1 = box.getMinY.max(other.getMinY)
      val z1 = box.getMinZ.max(other.getMinZ)
      val x2 = box.getMaxX.min(other.getMaxX)
      val y2 = box.getMaxY.min(other.getMaxY)
      val z2 = box.getMaxZ.min(other.getMaxZ)
      BlockBox(x1, y1, z1, x2, y2, z2)
    }
  }
  def getWallPositions(box : BlockBox) : Seq[BlockPos] = {
    val allPositions = getBlocksIn(box).toSet 
    val insidePositions = getInnerPositions(box).toSet 
    (allPositions -- insidePositions).toList
  }
  def getBoundsForBlocks(filled : Seq[BlockPos]): BlockBox = {
    if (filled.isEmpty)
      BlockBox(0,0,0,0,0,0)
    else {
      val iterable : java.lang.Iterable[BlockPos] = filled.asJava
      val trimmedBounds : Option[BlockBox] = BlockBox.encompassPositions(iterable).toScala
      trimmedBounds.getOrElse(BlockBox(0,0,0,0,0,0))
    }
  }
  def boundsFitsInside(inner : Box, outer : Box): Boolean = {
    inner.getZLength <= outer.getZLength
    && inner.getXLength <= outer.getXLength
    && inner.getYLength <= outer.getYLength
  }
  def boundsFitsInside(inner : BlockBox, outer : BlockBox) : Boolean = {
    inner.getBlockCountX <= outer.getBlockCountX
    && inner.getBlockCountY <= outer.getBlockCountY
    && inner.getBlockCountZ <= outer.getBlockCountZ
  }
  def getLayerBounds(box : BlockBox, layerOffset : Int): BlockBox = {
    BlockBox(box.getMinX, box.getMinY + layerOffset, box.getMinZ,
        box.getMaxX, box.getMaxY + layerOffset, box.getMaxZ
      )
  }
  def rotatePositions(positions : Seq[BlockPos], rotation : BlockRotation) : Map[BlockPos, BlockPos] = {
    val bounds = getBoundsForBlocks(positions)
    val rotatedPreNormalize : Map[BlockPos, BlockPos] = positions
      .map(p => (p, normalizeLayerPosition(bounds, p)))
      .map {case (p, r) => (p, r.rotate(rotation)) }
      .map {case (p, r) => (p, denormalizeLayerPosition(bounds, r).toImmutable)}
      .toMap
    val rotatedBounds = BlockSpaceUtil.getBoundsForBlocks(rotatedPreNormalize.values.toList) 
    rotatedPreNormalize.map((k, rp) => {
      val renormalized = normalizeLayerPosition(rotatedBounds, rp)
      val realPosition = denormalizeLayerPosition(rotatedBounds, renormalized)
      (k.toImmutable(), realPosition.toImmutable)
    })
  }
  def normalizeLayerPosition(bounds  :BlockBox, pos : BlockPos): BlockPos = {
    val center = bounds.getCenter
    BlockPos(pos.x - center.getX, pos.y - center.getY, pos.z - center.getZ)
  }

  def denormalizeLayerPosition(bounds  : BlockBox, pos : BlockPos) : BlockPos = {
    val center = bounds.getCenter
    BlockPos(pos.x + center.getX, pos.y + center.getY, pos.z + center.getZ)
  }
}

