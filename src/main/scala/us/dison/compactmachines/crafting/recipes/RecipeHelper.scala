package us.dison.compactmachines.crafting.recipes

import scala.collection.mutable
import us.dison.compactmachines.*
import net.minecraft.util.math.BlockPos
object RecipeHelper {
  def convertSingleListToMap(list: List[String], x: Int): Map[BlockPos, String] = {
    val map = mutable.Map[BlockPos, String]()
    for (z <- list.indices) {
      val s = list(z)
      val relative = BlockPos(x, 0, z)
      map.put(relative, s)
    }
    map.toMap
  }

  def convertMultiListToMap(list: List[List[String]]): Map[BlockPos, String] = {
    val map = mutable.Map[BlockPos, String]()
    for (x <- list.indices) {
      val zValues = list(x)
      map ++= convertSingleListToMap(zValues, x)
    }
    map.toMap
  }
}
