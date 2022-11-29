package us.dison.compactmachines.crafting.catalyst

import net.minecraft.item.{Item, ItemStack}
import net.minecraft.tag.TagKey
import net.minecraft.util.registry.Registry
import us.dison.compactmachines.api.crafting.catalyst.CatalystType
import us.dison.compactmachines.*
import com.mojang.serialization.codecs.RecordCodecBuilder

class ItemTagCatalystMatcher(val tag: TagKey[Item]) extends TCatalystMatcher {
  override def matches(stack: ItemStack): Boolean = {
    if (tag == null)
      true
    else
      stack.isIn(tag)
  }

  override def possible: Set[ItemStack] = {
    if (tag == null)
      Set()
    else
      val tag2 = Registry.ITEM.iterateEntries(tag).asScala
      tag2.map(it => ItemStack(it)).toSet
  }

  override def getType: CatalystType[?] = CompactMachinesCraftingPlugin.ITEM_TAG_CATALYST
}

object ItemTagCatalystMatcher {
  val codec: Codec[ItemTagCatalystMatcher] = RecordCodecBuilder.create(in => in.group(
    TagKey.identifierCodec(Registry.ITEM_KEY).fieldOf("tag").forGetter(_.tag)
  ).apply(in, it => ItemTagCatalystMatcher(it))
  )
}