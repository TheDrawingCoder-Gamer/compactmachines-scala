package us.dison.compactmachines.crafting.recipes

import com.google.common.collect.ImmutableList
import com.mojang.datafixers.util.Pair
import com.mojang.serialization.{Codec, DataResult, DynamicOps, Lifecycle}
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.api.crafting.components.IRecipeComponent
import us.dison.compactmachines.crafting.catalyst.{CatalystMatcherCodec, ItemStackCatalystMatcher}
import us.dison.compactmachines.crafting.recipes.layers.dim.IFixedSizedRecipeLayer
import us.dison.compactmachines.*
import us.dison.compactmachines.api.crafting.recipes.layers.IRecipeLayer
import us.dison.compactmachines.crafting.components.RecipeComponentTypeCodec
import java.util.Collections
import scala.collection.mutable

object MiniaturizationRecipeCodec extends Codec[MiniaturizationRecipe] {

  import layers.RecipeLayerTypeCodec

  val layerCodec: Codec[IRecipeLayer] = RecipeLayerTypeCodec.dispatchStable(_.getType, _.getCodec)
  val componentCodec: Codec[IRecipeComponent] =
    RecipeComponentTypeCodec.dispatchStable(_.getType(), _.getCodec())

  override def decode[T](ops: DynamicOps[T], input: T): DataResult[Pair[MiniaturizationRecipe, T]] = {
    val recipe = MiniaturizationRecipe()
    val errorBuilder = mutable.StringBuilder()
    val recipeSize = Codec.INT.optionalFieldOf("recipeSize", -1)
      .codec()
      .parse(ops, input)
      .result()
      .get()
    val canFitLayers = recipe.setRecipeSize(recipeSize)
    val layers = layerCodec.listOf()
      .fieldOf("layers").codec()
      .parse(ops, input)
    if (layers.error().isPresent) {
      val partialLayers = layers.resultOrPartial(errorBuilder.append)
      partialLayers.ifPresent(it => recipe.applyLayers(it.asScala.toList))
      CompactMachines.LOGGER.warn(errorBuilder.toString())
      DataResult.error(errorBuilder.toString(), Pair.of(recipe, input), Lifecycle.stable())
    } else {
      val layerList = layers.resultOrPartial(errorBuilder.append).orElse(Collections.emptyList()).asScala.toList
      recipe.applyLayers(layerList)
      val hasFixedLayers = layerList.exists(l => l.isInstanceOf[IFixedSizedRecipeLayer])
      if (!hasFixedLayers && !canFitLayers) {
        errorBuilder.append("Specified recipe size will not fit in a crafting field: ").append(recipeSize)
        CompactMachines.LOGGER.warn(errorBuilder.toString())
        DataResult.error(errorBuilder.toString(), Pair.of(recipe, input), Lifecycle.stable())
      } else {
        recipe.recalculateDimensions()
        val catalystNode = ops.get(input, "catalyst").result()
        if (catalystNode.isEmpty) {
          CompactMachines.LOGGER.warn("No catalyst node defined in recipe; Likely bad file!")
        } else {
          val catalystType = ops.get(catalystNode.get(), "type").result()
          if (catalystType.isEmpty) {
            CompactMachines.LOGGER.warn("Error : No catalyst type defined in recipe; falling back to itemstack")
            val stackData = ItemStack.CODEC
              .fieldOf("catalyst").codec()
              .parse(ops, input)
              .resultOrPartial(errorBuilder.append)
              .orElse(ItemStack.EMPTY)
            recipe.catalyst = ItemStackCatalystMatcher.ofStack(stackData)
          } else {
            val catalyst = CatalystMatcherCodec.matcherCodec
              .fieldOf("catalyst").codec()
              .parse(ops, input)
              .resultOrPartial(errorBuilder.append)
              .orElse(ItemStackCatalystMatcher.ofStack(ItemStack.EMPTY))
            recipe.catalyst = catalyst
          }
        }
        val outputs = ItemStack.CODEC.listOf()
          .fieldOf("outputs")
          .codec()
          .parse(ops, input)
          .resultOrPartial(errorBuilder.append)
        outputs.ifPresent(it => recipe.outputs = it.asScala.toList)
        if (!outputs.isPresent) {
          DataResult.error(errorBuilder.toString(), Pair.of(recipe, input), Lifecycle.stable())
        } else {
          if (recipe.outputs.isEmpty) {
            errorBuilder.append("No outputs were defined")
            CompactMachines.LOGGER.warn(errorBuilder.toString())
            DataResult.error(errorBuilder.toString(), Pair.of(recipe, input), Lifecycle.stable())
          } else {
            val components = Codec.unboundedMap(Codec.STRING, componentCodec)
              .optionalFieldOf("components", Collections.emptyMap())
              .codec()
              .parse(ops, input)
              .resultOrPartial(errorBuilder.append)
            components.ifPresent { compNode =>
              recipe.applyComponents(compNode.asScala.toMap)
            }
            DataResult.success(Pair.of(recipe, input), Lifecycle.stable())
          }
        }
      }
    }
  }

  override def encode[T](recipe: MiniaturizationRecipe, ops: DynamicOps[T], prefix: T): DataResult[T] = {
    if (recipe == null) {
      DataResult.error("Cannot serialize a null recipe")
    } else {
      val layers = layerCodec.listOf().encodeStart(ops, recipe.getLayerListForCodecWrite.asJava)
      val components = Codec.unboundedMap(Codec.STRING, componentCodec)
        .encodeStart(ops, recipe.components.getAllComponents)
      val catalystItem = recipe.catalyst
      val catalyst: Option[DataResult[T]] =
        if (catalystItem != null) {
          Some(CatalystMatcherCodec.matcherCodec.encodeStart(ops, catalystItem))
        } else {
          None
        }
      val outputs = ItemStack.CODEC.listOf()
        .encodeStart(ops, ImmutableList.copyOf(recipe.outputs.asJava))
      val builder = ops.mapBuilder()
      builder.add("type", Identifier.CODEC.encodeStart(ops, CompactMachines.ID_MINIATURIZATION_RECIPE))
      if (recipe.hasSpecifiedSize) {
        builder.add("recipeSize", Codec.INT.encodeStart(ops, recipe.getRecipeSize))
      }
      val b = builder.add("layers", layers)
        .add("components", components)
      catalyst.foreach(it => b.add("catalyst", it))
      b.add("outputs", outputs).build(prefix)
    }
  }
}
