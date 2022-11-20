package us.dison.compactmachines.api.crafting.recipes.layers;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.block.BlockState;
import java.util.Optional; 
import java.util.stream.Stream;
import java.util.Map;
public interface CCRecipeBlocks {
	Optional<String> getComponentAtPosition(BlockPos relative);
	Optional<BlockState> getStateAtPosition(BlockPos relative);
	Stream<BlockPos> getPositions();
	int getNumberKnownComponents();
	void rebuildComponentTotals();
	Map<String, Integer> getKnownComponentTotals();
	Box getSourceBounds();
	boolean allIdentified();
	Stream<BlockPos> getUnmappedPositions();
	Stream<BlockPos> getPositionsForComponent(String component);
	Box getFilledBounds();
	CCRecipeBlocks slice(Box bounds);
	CCRecipeBlocks offset(Vec3i amount);
	default CCRecipeBlocks below(int offset) {
		return offset(new Vec3i(0, -offset, 0));
	} 
	default CCRecipeBlocks above(int offset) {
		return offset(new Vec3i(0, offset, 0));
	}
	default CCRecipeBlocks normalize() {
		Box sb = getSourceBounds();
		BlockPos offset = new BlockPos(-sb.minX, -sb.minY, -sb.minZ);
		return offset(offset);
	}

}
