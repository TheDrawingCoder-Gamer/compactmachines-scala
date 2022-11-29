package us.dison.compactmachines.api.crafting.recipes.layers;

import com.mojang.serialization.Codec;
public interface RecipeLayerType<L extends IRecipeLayer> {
	Codec<L> getCodec();
}
