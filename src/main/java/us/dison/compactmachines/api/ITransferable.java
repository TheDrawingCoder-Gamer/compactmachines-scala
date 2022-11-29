package us.dison.compactmachines.api;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface ITransferable<T> {
    Identifier getName();
    T lookup(World world, BlockPos pos, Direction direction);
}
