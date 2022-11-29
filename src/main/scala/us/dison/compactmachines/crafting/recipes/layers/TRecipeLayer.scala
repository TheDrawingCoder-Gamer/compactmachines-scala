package us.dison.compactmachines.crafting.recipes.layers

import net.minecraft.util.math.BlockPos
import us.dison.compactmachines.*
import us.dison.compactmachines.api.crafting.recipes.layers.IRecipeLayer
import java.util
import java.util.{Optional, stream}

trait TRecipeLayer extends IRecipeLayer {
  override def getComponentTotals: util.Map[String, Integer] =
    componentTotals.asJava.asInstanceOf

  def componentTotals: Map[String, Int]

  override def getComponentForPosition(pos: BlockPos): Optional[String] =
    componentAtPosition(pos).toJava

  def componentAtPosition(pos: BlockPos): Option[String]

  override def getPositionsForComponent(key: String): stream.Stream[BlockPos] =
    positionsForComponent(key).asJava.stream()

  def positionsForComponent(componentKey: String): Seq[BlockPos]
}
