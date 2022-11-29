package us.dison.compactmachines.crafting.recipes

import net.minecraft.item.ItemStack
import net.minecraft.recipe.{RecipeSerializer, RecipeType}
import net.minecraft.util.math.{BlockBox, Box, Vec3d}
import net.minecraft.util.{BlockRotation, Identifier}
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.api.crafting.catalyst.ICatalystMatcher
import us.dison.compactmachines.api.crafting.components.{IRecipeBlockComponent, IRecipeComponent, IRecipeComponents}
import us.dison.compactmachines.api.crafting.field.FieldSize
import us.dison.compactmachines.api.crafting.recipes.layers.{IRecipeBlocks, IRecipeLayer, ISymmetricLayer}
import us.dison.compactmachines.crafting.recipes.layers.dim.{IDynamicSizedRecipeLayer, IFixedSizedRecipeLayer}
import us.dison.compactmachines.util.BlockSpaceUtil
import us.dison.compactmachines.*
import us.dison.compactmachines.crafting.components.IRecipeComponentsExt.*

import java.util
import scala.beans.BeanProperty
import scala.collection.mutable
import us.dison.compactmachines.crafting.components.*
class MiniaturizationRecipe extends CCRecipeBase with TMiniaturizationRecipe {
  private var recipeSize = -1
  @BeanProperty
  var id: Identifier = _
  private val layersMap = mutable.HashMap[Int, IRecipeLayer]()
  var catalyst: ICatalystMatcher = _
  override var outputs: List[ItemStack] = List[ItemStack]()
  private var internalDimensions : BlockBox = BlockBox(0,0,0, 0, 0, 0)
  private var cachedComponentTotals: Map[String, Int] = _
  override val components: IRecipeComponents = MiniaturizationRecipeComponents()

  override def identifier: Identifier = id

  // override def setId(recipeId: Identifier): Unit = id = recipeId
  override def dimensions: BlockBox = internalDimensions

  override def getLayerAt(layer: Int): Option[IRecipeLayer] =
    layersMap.get(layer)

  override def layers: List[IRecipeLayer] = layersMap.values.toList

  override def craftingTime: Int = 200

  import us.dison.compactmachines.crafting.recipes.layers.*

  def matchesBlocks(blocks: IRecipeBlocks): Boolean = {
    BlockSpaceUtil.boundsFitsInside(blocks.filledBounds, dimensions) && {
      // val filledBounds = blocks.filledBounds
      val validRotations = BlockRotation.values()
      val layerRotationMatches = mutable.HashMap[BlockRotation, mutable.HashSet[Int]]()
      validRotations.foreach { rot =>
        layerRotationMatches(rot) = mutable.HashSet()
      }
      for ((key, layer) <- layersMap.toMap) {
        val layerblocks = blocks.slice(BlockSpaceUtil.getLayerBounds(blocks.filledBounds, key)).normalize()
        if (layer.requiresAllBlocksIdentified() && !layerblocks.allIdentified()) {
          return false
        }
        val firstMatched = layer.matches(components, layerblocks)
        if (firstMatched) {
          layerRotationMatches(BlockRotation.NONE) += key
        }
        layer match {
          case _: ISymmetricLayer if dimensions.getBlockCountX == dimensions.getBlockCountZ =>
            if (firstMatched) {
              for (rot <- util.Arrays.stream(BlockRotation.values()).filter(_ != BlockRotation.NONE).toScala(List)) {
                layerRotationMatches(rot) += key
              }
            }

          case _ =>
            util.Arrays.stream(BlockRotation.values()).toScala(List).filter(_ != BlockRotation.NONE).map { rot =>
              val rotated = RecipeBlocks.rotate(layerblocks, rot)
              if (layer.matches(components, rotated)) {
                layerRotationMatches(rot) += key
              }
            }
        }


      }
      layerRotationMatches.filter { case (key, value) =>
        value == layersMap.keySet
      }.keys.nonEmpty

    }
  }

  def applyComponents(compMap: Map[String, IRecipeComponent]): Unit = {
    components.clear()
    for ((key, value) <- compMap) {
      value match {
        case v: IRecipeBlockComponent =>
          components.registerBlock(key, v)
      }
    }
    for ((i, l) <- layersMap) {
      l.dropNonRequiredComponents(components)
      val missing = l.getComponents
        .stream()
        .filter(key => !components.hasBlock(key))
        .toScala(Set)
      missing.foreach(needed => components.registerBlock(needed, EmptyBlockComponent()))
    }
  }

  def applyLayers(layers: List[IRecipeLayer]): Unit = {

    layersMap.clear()
    val rev = layers.reverse
    for (y <- rev.indices) {
      layersMap.put(y, rev(y))
    }
  }

  def getLayerListForCodecWrite: Seq[IRecipeLayer] = {
    val arr = layersMap.keys.toArray
    scala.util.Sorting.quickSort[Int](arr)(using math.Ordering.Int.reverse)
    arr.toList.map(layersMap.apply)
  }

  def recalculateDimensions(): Unit = {
    val height = layersMap.size
    var x = 0
    var z = 0

    val hasAnyRigidLayers = layersMap.values.exists(l => l.isInstanceOf[us.dison.compactmachines.crafting.recipes.layers.dim.IFixedSizedRecipeLayer])
    if (!hasAnyRigidLayers) {
      this.internalDimensions = BlockBox(0, 0, 0, recipeSize, height, recipeSize)
    } else {
      for (l <- layersMap.values) {
        l match {
          case layer: IFixedSizedRecipeLayer =>
            val dim = layer.dimensions
            if (dim.getBlockCountX > x)
              x = dim.getBlockCountX
            if (dim.getBlockCountZ > z)
              z = dim.getBlockCountZ
        }
      }
      this.internalDimensions = BlockBox(0,0,0, x, height, z)
    }
    updateFluidLayerDimensions()
  }

  def updateFluidLayerDimensions(): Unit = {
    val footprint = BlockSpaceUtil.getLayerBounds(dimensions, 0)
    layersMap.values
      .filter(l => l.isInstanceOf[IDynamicSizedRecipeLayer])
      .foreach(dl => dl.asInstanceOf[IDynamicSizedRecipeLayer].dimensions = footprint)

  }

  def fitsInFieldSize(fieldSize: FieldSize): Boolean = {
    val dim = fieldSize.getDimensions
    dimensions.getBlockCountX <= dim
      && dimensions.getBlockCountY <= dim
      && dimensions.getBlockCountZ <= dim
  }

  def getComponentTotals: Map[String, Int] = {
    if (this.cachedComponentTotals != null)
      this.cachedComponentTotals
    else {
      val totals = mutable.HashMap[String, Int]()
      components.allComponents.keySet.foreach { comp =>
        val count = getComponentRequiredCount(comp)
        totals.put(comp, count)
      }
      this.cachedComponentTotals = totals.toMap
      totals.toMap
    }
  }

  def getComponentRequiredCount(i: String): Int = {
    if (!components.hasBlock(i))
      0
    else {
      layersMap.values
        .map(_.getComponentTotals)
        .map(totals => totals.get(i))
        .reduce(_ + _)
    }
  }

  def getNumberLayers: Int = {
    layersMap.size
  }

  def getRecipeSize: Int = recipeSize

  def setRecipeSize(size: Int): Boolean =
    if (FieldSize.canFitDimensions(size)) {
      this.recipeSize = size
      recalculateDimensions()
      true
    } else false

  def hasSpecifiedSize: Boolean =
    FieldSize.canFitDimensions(this.recipeSize)

  // override def getRecipeIdentifier() = this.id
  override def getSerializer: RecipeSerializer[_] =
    MiniaturizationRecipeSerializer

  override def getType: RecipeType[_] =
    CompactMachines.TYPE_MINIATURIZATION_RECIPE
}
