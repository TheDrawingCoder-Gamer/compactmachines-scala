package us.dison.compactmachines.api.crafting.field;

import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;

import java.util.Arrays;
public enum FieldSize implements StringIdentifiable {
	INACTIVE("inactive", 0, 0),
	SMALL("small", 1, 3),
	MEDIUM("medium", 2, 5),
	LARGE("large", 3, 7),
	ABSURD("absurd", 4, 9);
	private final int size;
	private final int projectorDistance;
	private final String name;
	public static final Codec<FieldSize> CODEC = 
		Codec.STRING.xmap(FieldSize::valueOf, FieldSize::name);
	public static final FieldSize[] VALID_SIZES = new FieldSize[] {
		SMALL, MEDIUM, LARGE, ABSURD 
	};

	FieldSize(String name, int size, int distance) {
		this.name = name; 
		this.size = size;
		this.projectorDistance = distance;
	}
	public int getDimensions() {
		return (size * 2) + 1;
	}
	@Override 
	public String asString() {
		return this.name;
	}

	public BlockPos getCenterFromProjector(BlockPos pos, Direction facing) {
		return pos.offset(facing, projectorDistance + 1);
	}
	public BlockPos getProjLocationWithDirection(BlockPos center, Direction facing) {
		return center.offset(facing, projectorDistance + 1);
	}
	public BlockPos getOriginCenter() {
		return BlockPos.ORIGIN.toImmutable();
	}
	public Stream<BlockPos> getProjectorPoses() {
		return Arrays.stream(new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST })
			.map(it -> getProjLocationWithDirection(getOriginCenter(), it));
	}
	public Stream<BlockPos> getProjectorPosesAt(BlockPos pos) {
		return getProjectorPoses().map( it -> it.add(pos));
	}

	public BlockPos getOppositeProjector(BlockPos pos, Direction facing) {
		final BlockPos center = getCenterFromProjector(pos, facing);
		return getProjLocationWithDirection(center, facing);
	}
	public Stream<BlockPos> getProjectorPosesOnAxis(BlockPos center, Direction.Axis axis) {
		if (axis == Direction.Axis.Y) {
			return Stream.empty();
		}
		return Arrays.stream(Direction.values()).filter(axis)
			.map(it -> getProjLocationWithDirection(center, it));
	}

	public static boolean canFitDimensions(int i) {
		return (i <= ABSURD.getDimensions() && i >= SMALL.getDimensions());
	}
	public BlockBox getBoundsAtPosition(BlockPos center) {
		return new BlockBox(center).expand(size);
	}
	public BlockPos getBoundsAsBlockPos() {
		final int dims = getDimensions();
		return new BlockPos(dims, dims, dims);
	}
	public int getProjectorDistance() {
		return this.projectorDistance;
	}
	public int getSize() {
		return this.size;
	}
	public String getName() {
		return this.name;
	}
}
