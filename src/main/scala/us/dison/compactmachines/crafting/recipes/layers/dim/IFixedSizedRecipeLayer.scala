package us.dison.compactmachines.crafting.recipes.layers.dim

import net.minecraft.util.math.{BlockBox, Box}

trait IFixedSizedRecipeLayer {
  def dimensions: BlockBox
}
