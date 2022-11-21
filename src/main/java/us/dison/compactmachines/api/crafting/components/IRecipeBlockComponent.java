package us.dison.compactmachines.api.crafting.components;

import net.minecraft.block.BlockState; 
import net.minecraft.block.Block; 
public interface IRecipeBlockComponent extends IRecipeComponent {
	boolean matches(BlockState state);
	Block getBlock();
	BlockState getRenderState();
	boolean didErrorRendering();
	void markRenderingError();
}
