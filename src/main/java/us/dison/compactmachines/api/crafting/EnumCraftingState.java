package us.dison.compactmachines.api.crafting;

import com.mojang.serialization.Codec;
public enum EnumCraftingState {
	NOT_MATCHED,
	MATCHED, 
	CRAFTING;
	public static final Codec<EnumCraftingState> CODEC = 
		Codec.STRING.xmap(EnumCraftingState::valueOf, EnumCraftingState::name);
}
