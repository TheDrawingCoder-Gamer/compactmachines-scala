package us.dison.compactmachines.api.crafting; 

import net.minecraft.util.math.BlockBox;
import us.dison.compactmachines.api.crafting.catalyst.ICatalystMatcher;
import java.util.stream.Stream;
import java.util.Collection;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import java.util.Optional;
import us.dison.compactmachines.api.crafting.recipes.layers.IRecipeLayer;
import us.dison.compactmachines.api.crafting.components.IRecipeComponents;

public interface IMiniaturizationRecipe {
	ICatalystMatcher getCatalyst();
	ItemStack[] getOutputs();
	Identifier getRecipeId();
	int getCraftingTime();
	BlockBox getDimensions();
	Optional<IRecipeLayer> getLayer(int layer);
	IRecipeComponents getComponents();
	void setOutputs(Collection<ItemStack> outputs);
	Stream<IRecipeLayer> getLayers();

}
