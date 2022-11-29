package us.dison.compactmachines.transfer

import team.reborn.energy.api.EnergyStorage
import net.minecraft.world.World
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.minecraft.util.Identifier
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.api.ITransferable
//noinspection UnstableApiUsage
object EnergyTransferable extends ITransferable[EnergyStorage] {
  override val getName: Identifier = Identifier(CompactMachines.MOD_ID, "energy")
  override def lookup(world : World, pos : BlockPos, dir : Direction) : EnergyStorage = {
    EnergyStorage.SIDED.find(world, pos, dir)
  }
}

//noinspection UnstableApiUsage
object ItemTransferable extends ITransferable[Storage[ItemVariant]] {
  override val getName: Identifier = Identifier("minecraft:item")
  override def lookup(world : World, pos : BlockPos, dir  : Direction) : Storage[ItemVariant] = ItemStorage.SIDED.find(world, pos, dir)
}
//noinspection UnstableApiUsage
object FluidTransferable extends ITransferable[Storage[FluidVariant]] {
  override val getName: Identifier = Identifier("minecraft:fluid")
  override def lookup(world : World, pos : BlockPos, dir : Direction) : Storage[FluidVariant] = FluidStorage.SIDED.find(world, pos, dir)
}
