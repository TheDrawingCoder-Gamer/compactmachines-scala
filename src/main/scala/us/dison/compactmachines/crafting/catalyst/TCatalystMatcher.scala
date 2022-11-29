package us.dison.compactmachines.crafting.catalyst

import net.minecraft.item.ItemStack
import us.dison.compactmachines.api.crafting.catalyst.ICatalystMatcher
import us.dison.compactmachines.*

import java.util
trait TCatalystMatcher extends ICatalystMatcher {
  def possible: Set[ItemStack]

  override def getPossible: util.Set[ItemStack] = possible.asJava
}
