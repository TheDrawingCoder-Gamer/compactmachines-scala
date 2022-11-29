package us.dison.compactmachines.api.crafting.recipes.layers;

import net.minecraft.util.math.*;
import net.minecraft.block.BlockState;
import java.util.Optional; 
import java.util.stream.Stream;
import java.util.Map;
public interface IRecipeBlocks {
	Optional<String> getComponentAtPosition(BlockPos relative);
	Optional<BlockState> getStateAtPosition(BlockPos relative);
	Stream<BlockPos> getPositions();
	int getNumberKnownComponents();
	void rebuildComponentTotals();
	Map<String, Integer> getKnownComponentTotals();
	BlockBox getSourceBounds();
	boolean allIdentified();
	Stream<BlockPos> getUnmappedPositions();
	Stream<BlockPos> getPositionsForComponent(String component);
	BlockBox getFilledBounds();
	IRecipeBlocks slice(BlockBox bounds);
	IRecipeBlocks offset(Vec3i amount);
	default IRecipeBlocks below(int offset) {
		return offset(new Vec3i(0, -offset, 0));
	} 
	default IRecipeBlocks above(int offset) {
		return offset(new Vec3i(0, offset, 0));
	}
	default IRecipeBlocks normalize() {
		BlockBox sb = getSourceBounds();
		final BlockPos center = sb.getCenter();
		BlockPos offset = new BlockPos(-center.getX(), -center.getY(), -center.getZ());
		return offset(offset);
	}

}
