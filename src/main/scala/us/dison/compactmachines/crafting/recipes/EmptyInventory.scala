package us.dison.compactmachines.crafting.recipes

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

trait EmptyInventory extends Inventory {
  override val size = 0
  override val isEmpty = true

  private def throwEmpty(): Nothing =
    throw UnsupportedOperationException("This inventory is empty!")

  override def getStack(slot: Int): ItemStack = throwEmpty()

  override def removeStack(slot: Int, amount: Int): ItemStack = throwEmpty()

  override def removeStack(slot: Int): ItemStack = throwEmpty()

  override def setStack(slot: Int, stack: ItemStack): Unit = throwEmpty()

  override def markDirty(): Unit = ()

  override def canPlayerUse(player: PlayerEntity) = false

  override def clear(): Unit = ()
}
