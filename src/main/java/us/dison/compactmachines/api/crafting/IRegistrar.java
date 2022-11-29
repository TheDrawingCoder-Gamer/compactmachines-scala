package us.dison.compactmachines.api.crafting;

import us.dison.compactmachines.api.crafting.catalyst.CatalystType;
import us.dison.compactmachines.api.crafting.components.RecipeComponentType;
import us.dison.compactmachines.api.crafting.recipes.layers.RecipeLayerType;
import net.minecraft.util.Identifier;
public interface IRegistrar {
	void registerLayerType(Identifier id, RecipeLayerType<?> layerType);
	void registerComponentType(Identifier id, RecipeComponentType<?> componentType);
	void registerCatalystType(Identifier id, CatalystType<?> catalystType);
}
