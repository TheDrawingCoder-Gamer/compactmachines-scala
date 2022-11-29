package us.dison.compactmachines.crafting.recipes

import net.minecraft.recipe.RecipeType
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

class CCBaseRecipeType[T <: CCRecipeBase](private val location: Identifier) extends RecipeType[T] {
  override def toString: String = location.toString

  def register(): CCBaseRecipeType[T] = {
    Registry.register(Registry.RECIPE_TYPE, location, this)
  }
}
