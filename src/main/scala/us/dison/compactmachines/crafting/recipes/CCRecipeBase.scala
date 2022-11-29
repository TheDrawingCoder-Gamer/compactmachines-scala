package us.dison.compactmachines.crafting.recipes

import net.minecraft.item.ItemStack
import net.minecraft.recipe.Recipe
import net.minecraft.util.Identifier
import net.minecraft.world.World

trait CCRecipeBase extends Recipe[EmptyInventory] {
  override def matches(inv: EmptyInventory, worldIn: World) = true

  override def craft(inv: EmptyInventory): ItemStack = ItemStack.EMPTY

  override def fits(width: Int, height: Int) = true

  override def getOutput: ItemStack = ItemStack.EMPTY

  override def isIgnoredInRecipeBook = true

  def setId(recipeId: Identifier): Unit
}
