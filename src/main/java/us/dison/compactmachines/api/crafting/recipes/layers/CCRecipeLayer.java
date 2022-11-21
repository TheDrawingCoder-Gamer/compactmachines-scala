package us.dison.compactmachines.api.crafting.recipes.layers;

import java.util.Set;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.math.BlockPos;
import us.dison.compactmachines.api.crafting.components.IRecipeComponents;
import us.dison.compactmachines.api.crafting.recipes.layers.CCRecipeBlocks;
import us.dison.compactmachines.api.crafting.recipes.layers.RecipeLayerType ;
public interface CCRecipeLayer {
	Set<String> getComponents();
	Map<String, Integer> getComponentTotals();
	Optional<String> getComponentForPosition(BlockPos pos);
	Stream<BlockPos> getPositionsForComponent(String component);
	default boolean requiresAllBlocksIdentified() {
		return true;
	}
	default boolean matches(IRecipeComponents components, CCRecipeBlocks blocks) {
		return !requiresAllBlocksIdentified() || blocks.allIdentified();
	}
	default void dropNonRequiredComponents(IRecipeComponents components) {}
	RecipeLayerType<?> getType();

}
