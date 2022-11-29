package us.dison.compactmachines.crafting.recipes.layers.givens

import us.dison.compactmachines.*
import net.minecraft.util.math.{BlockBox, Box}
import us.dison.compactmachines.api.crafting.recipes.layers.IRecipeBlocks

import java.util.stream.Collectors

object ccRecipeBlocks {
extension (c: IRecipeBlocks) {
  def componentAtPosition(relative: BlockPos): Option[String] =
    c.getComponentAtPosition(relative).toScala
  def stateAtPosition(relative: BlockPos): Option[BlockState] =
    c.getStateAtPosition(relative).toScala
  def positions: List[BlockPos] =
    c.getPositions.collect(Collectors.toList).asScala.toList
  def numberKnownComponents: Int =
    c.getNumberKnownComponents
  def knownComponentTotals: Map[String, Int] =
    c.getKnownComponentTotals.asScala.toMap.view.mapValues(_.toInt).toMap
  def sourceBounds: BlockBox =
    c.getSourceBounds
  def unmappedPositions: List[BlockPos] =
    c.getUnmappedPositions.toScala(List)
  def positionsForComponent(component: String): List[BlockPos] =
    c.getPositionsForComponent(component).toScala(List)
  def filledBounds: BlockBox = c.getFilledBounds
}
}

