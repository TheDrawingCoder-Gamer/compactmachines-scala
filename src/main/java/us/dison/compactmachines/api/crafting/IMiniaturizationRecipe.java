package us.dison.compactmachines.api.crafting; 

import us.dison.compactmachines.api.crafting.catalyst.ICatalystMatcher;
import java.util.stream.Stream;
import java.util.Collection;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import java.util.Optional;
import us.dison.compactmachines.api.crafting.recipes.layers.CCRecipeLayer;
import us.dison.compactmachines.api.crafting.components.IRecipeComponents;

public interface IMiniaturizationRecipe {
	ICatalystMatcher getCatalyst();
	ItemStack[] getOutputs();
	Identifier getRecipeId();
	int getCraftingTime();
	Box getDimensions();
	Optional<CCRecipeLayer> getLayer(int layer);
	IRecipeComponents getComponents();
	void setOutputs(Collection<ItemStack> outputs);
	Stream<CCRecipeLayer> getLayers();

}
