package us.dison.compactmachines.api.crafting.components;

import com.mojang.serialization.Codec;
public interface RecipeComponentType<C extends IRecipeComponent> {
	Codec<C> getCodec();
}
