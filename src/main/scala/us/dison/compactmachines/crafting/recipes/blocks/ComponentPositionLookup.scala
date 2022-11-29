package us.dison.compactmachines.crafting.recipes.blocks

import us.dison.compactmachines.*
import scala.collection.mutable
class ComponentPositionLookup {
  protected[recipes] val componentsMap: mutable.Map[BlockPos, String] = mutable.Map[BlockPos, String]()
  protected[recipes] val componentTotalsMap: mutable.Map[String, Int] = mutable.Map[String, Int]()

  def add(location: BlockPos, component: String): Unit = {
    componentsMap.updateWith(location)({
      case None => Some(component)
      case Some(c) => Some(c)
    })
    componentTotalsMap.updateWith(component)({
      case None => Some(1)
      case Some(c) => Some(c + 1)
    })

  }

  def components: scala.collection.Set[String] = {
    componentTotalsMap.keySet
  }

  def allPositions: Seq[BlockPos] = componentsMap.keys.toList

  def containsLocation(loc: BlockPos): Boolean = componentsMap.contains(loc)

  def componentTotals: Map[String, Int] = {
    componentTotalsMap.toMap
  }

  protected[recipes] def rebuildComponentTotals(): Unit = {
    componentTotalsMap.clear()
    val totals = componentsMap.values.groupMapReduce(identity)(_ => 1)(_ + _)
    componentTotalsMap ++= totals
  }

  def getRequiredComponentKeyForPos(pos: BlockPos): Option[String] = componentsMap.get(pos)

  def getPositionsForComponent(comp: String): List[BlockPos] = {
    Option(comp) match {
      case None => List()
      case Some(c) =>
        componentsMap.iterator.filter(e => e._2 == c)
          .map(_._1)
          .toList
    }
  }

  def remove(comp: String): Option[Int] = {
    val positions = getPositionsForComponent(comp)
    positions.foreach(componentsMap -= _)
    componentTotalsMap.remove(comp)
  }
}
