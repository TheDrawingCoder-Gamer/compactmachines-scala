package us.dison.compactmachines.transfer 

import us.dison.compactmachines.api.ITransferable
import team.reborn.energy.api.EnergyStorage
import net.minecraft.world.World
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.util.Identifier
object EnergyTransferable extends ITransferable[EnergyStorage] {
  override val name = Identifier("compactmachines:energy")
  override protected def lookup(world : World, pos : BlockPos, dir : Direction) : EnergyStorage = {
    EnergyStorage.SIDED.find(world, pos, dir)
  }
}

object ItemTransferable extends ITransferable[Storage[ItemVariant]] {
  override val name = Identifier("minecraft:item")
  override protected def lookup(world : World, pos : BlockPos, dir  : Direction) : Storage[ItemVariant] = ItemStorage.SIDED.find(world, pos, dir)
}

object FluidTransferable extends ITransferable[Storage[FluidVariant]] {
  override val name = Identifier("minecraft:fluid")
  override protected def lookup(world : World, pos : BlockPos, dir : Direction) : Storage[FluidVariant] = FluidStorage.SIDED.find(world, pos, dir)
}
