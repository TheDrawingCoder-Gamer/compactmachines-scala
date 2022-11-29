package us.dison.compactmachines.crafting.recipes.layers

import net.minecraft.util.math.{BlockPos, Box, BlockBox}
import us.dison.compactmachines.api.crafting.components.IRecipeComponents
import us.dison.compactmachines.api.crafting.recipes.layers.{IRecipeBlocks, RecipeLayerType}
import us.dison.compactmachines.crafting.recipes.blocks.ComponentPositionLookup
import us.dison.compactmachines.crafting.recipes.layers.dim.IFixedSizedRecipeLayer
import us.dison.compactmachines.util.BlockSpaceUtil
import us.dison.compactmachines.*
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.util
import scala.beans.BeanProperty
import us.dison.compactmachines.crafting.recipes.blocks.ComponentPositionLookupCodec

class MixedComponentRecipeLayer(val componentLookup: ComponentPositionLookup = ComponentPositionLookup()) extends TRecipeLayer, IFixedSizedRecipeLayer {
  @BeanProperty
  val dimensions: BlockBox = BlockSpaceUtil.getBoundsForBlocks(componentLookup.allPositions)

  // override def getDimensions = dimensions
  override def getComponents: util.Set[String] = componentLookup.components.asJava

  override def dropNonRequiredComponents(components: IRecipeComponents): Unit = {
    components.getEmptyComponents.forEach(it => componentLookup.remove(it))
    val definedKeys = components.getBlockComponents.keySet
    val toRemove = componentLookup.components
      .filter(layerComp => !definedKeys.contains(layerComp))
    toRemove.foreach(it => componentLookup.remove(it))
  }

  override def componentTotals: Map[String, Int] =
    componentLookup.componentTotals

  override def componentAtPosition(pos: BlockPos): Option[String] = {
    componentLookup.getRequiredComponentKeyForPos(pos)
  }

  override def positionsForComponent(comp: String): Seq[BlockPos] = {
    componentLookup.getPositionsForComponent(comp)
  }

  def numberFilledPositions: Int = {
    componentTotals.values.sum
  }

  override def matches(components: IRecipeComponents, blocks: IRecipeBlocks): Boolean = {
    if (!blocks.allIdentified()) {
      val anyNonAir = blocks.unmappedPositions
        .flatMap(it => blocks.stateAtPosition(it))
        .exists(state => !state.isAir)
      if (anyNonAir) return false
    }
    val requiredKeys = componentLookup.components
    val componentTotals = blocks.knownComponentTotals

    val missingComponents : Set[String] = requiredKeys.filter(rk => !componentTotals.contains(rk)).toSet
    if (missingComponents.nonEmpty) {
      false
    } else {
      requiredKeys.forall { required =>
        val actual : Set[BlockPos] = blocks.getPositionsForComponent(required).toScala(Set)
        val expected : Set[BlockPos] = componentLookup.getPositionsForComponent(required).toSet
        expected == actual
      }
      /*
      for (required <- requiredKeys) {
        val actual = blocks.getPositionsForComponent(required)
        val expected = componentLookup.getPositionsForComponent(required)
        if (expected != actual) {
          CompactMachines.LOGGER.info("required key not found")
          return false
        }
      }
      */
    }
  }

  override def getType: RecipeLayerType[_] = MixedComponentRecipeLayer
}

object MixedComponentRecipeLayer extends RecipeLayerType[MixedComponentRecipeLayer] {
  override val getCodec: Codec[MixedComponentRecipeLayer] = RecordCodecBuilder.create(in => in.group(
    ComponentPositionLookupCodec
      .fieldOf("pattern")
      .forGetter(_.componentLookup)
  ).apply(in, it => MixedComponentRecipeLayer(it)))

}