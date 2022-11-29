package us.dison.compactmachines.util

import net.minecraft.entity.ItemEntity

object CraftingHelper {
  def consumeCatalystItem(item : ItemEntity, count : Int) : Boolean = {
    val stack = item.getStack

    if (stack.getCount < count)
      false 
    else if (stack.getCount == count) {
      item.discard()
      true 
    } else {
      stack.setCount(stack.getCount - count)
      item.setStack(stack.copy())
      true
    }
  }
}
