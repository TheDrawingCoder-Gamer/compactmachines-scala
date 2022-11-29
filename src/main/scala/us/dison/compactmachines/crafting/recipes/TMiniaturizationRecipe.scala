package us.dison.compactmachines.crafting.recipes

import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.{BlockBox, Box}
import us.dison.compactmachines.api.crafting.IMiniaturizationRecipe
import us.dison.compactmachines.api.crafting.catalyst.ICatalystMatcher
import us.dison.compactmachines.api.crafting.components.IRecipeComponents
import us.dison.compactmachines.*
import us.dison.compactmachines.api.crafting.recipes.layers.IRecipeLayer

import java.util.{Optional, stream}

trait TMiniaturizationRecipe extends IMiniaturizationRecipe {
  override def getCatalyst: ICatalystMatcher = catalyst

  def catalyst: ICatalystMatcher

  override def getOutputs: Array[ItemStack] = outputs.toArray

  def outputs: Seq[ItemStack]

  override def getRecipeId: Identifier = identifier

  def identifier: Identifier

  override def getCraftingTime: Int = craftingTime

  def craftingTime: Int

  override def getDimensions: BlockBox = dimensions

  def dimensions: BlockBox

  override def getLayer(layer: Int): Optional[IRecipeLayer] = getLayerAt(layer).toJava

  def getLayerAt(layer: Int): Option[IRecipeLayer]

  override def getComponents: IRecipeComponents = components

  def components: IRecipeComponents

  override def setOutputs(outputs: java.util.Collection[ItemStack]): Unit =
    this.outputs = outputs.asScala.toList

  def outputs_=(outputs: List[ItemStack]): Unit

  override def getLayers: stream.Stream[IRecipeLayer] =
    layers.asJavaSeqStream

  def layers: List[IRecipeLayer]
}
