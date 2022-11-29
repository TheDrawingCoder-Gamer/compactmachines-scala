package us.dison.compactmachines.crafting.catalyst

import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.{NbtCompound, NbtElement}
import net.minecraft.util.Identifier
import us.dison.compactmachines.api.crafting.catalyst.CatalystType
import scala.jdk.CollectionConverters._
import net.minecraft.util.registry.Registry
import com.mojang.serialization.codecs.RecordCodecBuilder
import scala.util.chaining.*
import us.dison.compactmachines.*
import us.dison.compactmachines.util.givens.codec.*

/*
trait CatalystType[M <: ICatalystMatcher] {
  def getCodec() : Codec[M]
}
*/
class ItemStackCatalystMatcher protected(val item: Item, val nbtMatcher: ItemStack => Boolean, val nbtTag: Option[NbtCompound]) extends TCatalystMatcher {
  private def getNbtTag = nbtTag

  def matches(stack: ItemStack): Boolean = {
    stack.getItem == item && nbtMatcher(stack)
  }

  override def getType: CatalystType[?] = CompactMachinesCraftingPlugin.ITEM_STACK_CATALYST

  override def possible: Set[ItemStack] = Set(ItemStack(item).tap(x => nbtTag.foreach(it => x.setNbt(it))))
}

object ItemStackCatalystMatcher {
  val codec: Codec[ItemStackCatalystMatcher] = RecordCodecBuilder.create(in => in.group(
    Identifier.CODEC.fieldOf("item").forGetter((x: ItemStackCatalystMatcher) => Registry.ITEM.getId(x.item)),
    NbtCompound.CODEC.optionFieldOf("nbt").forGetter((it: ItemStackCatalystMatcher) => it.getNbtTag)
  ).apply(in, ItemStackCatalystMatcher.of))

  def empty: ItemStackCatalystMatcher = ItemStackCatalystMatcher(null, _ => true, None)

  private def buildMatcher(filter: NbtCompound): ItemStack => Boolean = {
    if (filter == null)
      stack => true
    else {
      stack => {
        if (!stack.hasNbt && !filter.isEmpty)
          false
        else
          tagMatched(stack.getNbt, filter)
      }
    }
  }

  private def tagMatched(node: NbtCompound, filter: NbtCompound): Boolean = {
    if (filter.isEmpty)
      true
    else {
      filter.getKeys.stream().allMatch { key =>
        if (!node.contains(key))
          false
        else {
          val tagType = filter.getType(key)
          if (tagType == NbtElement.COMPOUND_TYPE) {
            tagMatched(node.getCompound(key), filter.getCompound(key))
          } else {
            val primitive = node.get(key)
            if (primitive == null)
              false
            else
              primitive == filter.get(key)
          }
        }
      }
    }
  }

  def ofStack(itemStack: ItemStack): ItemStackCatalystMatcher =
    ItemStackCatalystMatcher(itemStack.getItem, buildMatcher(itemStack.getNbt), Option(itemStack.getNbt))

  def of(item: Identifier, nbt: Option[NbtCompound]): ItemStackCatalystMatcher =
    ItemStackCatalystMatcher(Registry.ITEM.get(item), buildMatcher(nbt.orNull), nbt)

}