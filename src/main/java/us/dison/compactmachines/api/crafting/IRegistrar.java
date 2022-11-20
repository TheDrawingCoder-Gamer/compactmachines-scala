package us.dison.compactmachines.api.crafting;

import us.dison.compactmachines.api.crafting.recipes.layers.RecipeLayerType;
import net.minecraft.util.Identifier;
public interface IRegistrar {
	void registerLayerType(Identifier id, RecipeLayerType<?> layerType);

}
