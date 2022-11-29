package us.dison.compactmachines.crafting.recipes.blocks

import us.dison.compactmachines.crafting.recipes.RecipeHelper
import com.mojang.serialization.codecs.PrimitiveCodec
import us.dison.compactmachines.*
import us.dison.compactmachines.util.BlockSpaceUtil
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.Codec
import scala.collection.mutable
object ComponentPositionLookupCodec extends PrimitiveCodec[ComponentPositionLookup] {
  override def read[T](ops: DynamicOps[T], input: T): DataResult[ComponentPositionLookup] = {
    Codec.STRING.listOf().listOf().decode(ops, input).flatMap { s =>
      val layerList = s.getFirst
      val lookup = ComponentPositionLookup()

      val zSize = layerList.size
      val mappedArray: Seq[Seq[String]] =
        for (z <- 0 until zSize) yield {
          val layerComps = layerList.get(z)
          for (x <- 0 until layerComps.size) yield {
            layerComps.get(x)
          }
        }
      lookup.componentsMap ++= RecipeHelper.convertMultiListToMap(mappedArray.map(_.toList).toList)
      lookup.rebuildComponentTotals()

      DataResult.success(lookup)

    }
  }

  override def write[T](ops: DynamicOps[T], lookup: ComponentPositionLookup): T = {
    val boundsForBlocks = BlockSpaceUtil.getBoundsForBlocks(lookup.allPositions)
    val goodList = mutable.ListBuffer.fill[String](boundsForBlocks.getBlockCountX, boundsForBlocks.getBlockCountZ)("-")
    BlockSpaceUtil.getBlocksIn(boundsForBlocks)
      .map(pos => (pos.toImmutable, lookup.getRequiredComponentKeyForPos(pos).getOrElse("-")))
      .foreach { case (pos, comp) =>
        goodList(pos.getX).update(pos.getZ, comp)
      }
    val fin = goodList.map(_.asJava).asJava
    val encoded = Codec.STRING.listOf().listOf().encode(fin, ops, ops.empty())
    encoded.resultOrPartial(err => CompactMachines.LOGGER.error("Failed to encode layer component position lookup: {}", err)).get()
  }
}