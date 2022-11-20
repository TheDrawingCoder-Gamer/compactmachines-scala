package us.dison.compactmachines.util 

import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import scala.jdk.CollectionConverters._
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.BlockBox
import scala.jdk.OptionConverters._
import net.minecraft.util.BlockRotation
object BlockSpaceUtil {
  def getBlocksIn(box : Box) : List[BlockPos] = List.from(BlockPos.stream(box.shrink(1, 1, 1)).toList.iterator().asScala).map(_.toImmutable)
  def getInnerPositions(box : Box) : List[BlockPos] = {
    val inner = box.shrink(2, 0, 2).offset(1, 0, 1)
    getBlocksIn(inner)
  }
  def getWallPositions(box : Box) : List[BlockPos] = {
    val allPositions = getBlocksIn(box).toSet 
    val insidePositions = getInnerPositions(box).toSet 
    (allPositions -- insidePositions).toList
  }
  def getBoundsForBlocks(filled : List[BlockPos]) = {
    if (filled.size == 0) 
      Box.of(Vec3d.ZERO,0,0,0)
    else {
      val iterable : java.lang.Iterable[BlockPos] = filled.asJava
      val trimmedBounds : Option[BlockBox] = BlockBox.encompassPositions(iterable).asScala
      trimmedBounds.map(it => Box.from(it)).getOrElse(Box.of(Vec3d.ZERO,0,0,0))
    }
  }
  def boundsFitsInside(inner : Box, outer : Box) = 
    inner.getZLength() <= outer.getZLength()
    && inner.getXLength() <= outer.getXLength()
    && inner.getYLength() <= outer.getYLength()
  def getLayerBounds(box : Box, layerOffset : Int) = {
    Box(Vec3d(box.minX, box.minY + layerOffset, box.minZ),
        Vec3d(box.maxX, box.maxY + layerOffset, box.maxZ)
      )
  }
  def rotatePositions(positions : List[BlockPos], rotation : BlockRotation) : Map[BlockPos, BlockPos] = {
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
      (k.toImmutable(), realPosition.toImmutable())
    })
  }
  def normalizeLayerPosition(bounds  :Box, pos : BlockPos) = 
    BlockPos(pos.getX() - bounds.minX, pos.getY() - bounds.minY, pos.getZ() - bounds.minZ)
  def denormalizeLayerPosition(bounds  :Box, pos : BlockPos) = 
    BlockPos(pos.getX() + bounds.minX, pos.getY() + bounds.minY, pos.getZ() + bounds.minZ)
}

