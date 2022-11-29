package us.dison.compactmachines

import net.minecraft.util.Identifier
import us.dison.compactmachines.api.crafting.catalyst.CatalystType
import us.dison.compactmachines.api.crafting.components.RecipeComponentType
import us.dison.compactmachines.api.crafting.{CompactCraftingPlugin, IRegistrar}
import us.dison.compactmachines.crafting.catalyst.{ItemStackCatalystMatcher, ItemTagCatalystMatcher}
import us.dison.compactmachines.crafting.components.{BlockComponent, EmptyBlockComponent}
import us.dison.compactmachines.crafting.recipes.layers.{FilledComponentRecipeLayer, HollowComponentRecipeLayer, MixedComponentRecipeLayer}

object CompactMachinesCraftingPlugin extends CompactCraftingPlugin {
  val BLOCK_COMPONENT : RecipeComponentType[BlockComponent] = () => BlockComponent.codec
  val EMPTY_BLOCK_COMPONENT : RecipeComponentType[EmptyBlockComponent] = () => EmptyBlockComponent.codec

  val ITEM_STACK_CATALYST : CatalystType[ItemStackCatalystMatcher] = () => ItemStackCatalystMatcher.codec
  val ITEM_TAG_CATALYST : CatalystType[ItemTagCatalystMatcher] = () => ItemTagCatalystMatcher.codec

  override def register(registrar : IRegistrar): Unit = {
    registrar.registerLayerType(Identifier(CompactMachines.MOD_ID, "filled"), FilledComponentRecipeLayer)
    registrar.registerLayerType(Identifier(CompactMachines.MOD_ID, "hollow"), HollowComponentRecipeLayer)
    registrar.registerLayerType(Identifier(CompactMachines.MOD_ID, "mixed"), MixedComponentRecipeLayer)
    registrar.registerComponentType(Identifier(CompactMachines.MOD_ID, "block"), BLOCK_COMPONENT)
    registrar.registerComponentType(Identifier(CompactMachines.MOD_ID, "empty"), EMPTY_BLOCK_COMPONENT)
    registrar.registerCatalystType(Identifier(CompactMachines.MOD_ID, "item"), ITEM_STACK_CATALYST)
    registrar.registerCatalystType(Identifier(CompactMachines.MOD_ID, "tag"), ITEM_TAG_CATALYST)
  }
}