package us.dison.compactmachines.crafting.catalyst 

import net.minecraft.item.ItemStack
import com.mojang.serialization.Codec
import scala.collection.mutable.HashMap
import net.minecraft.util.Identifier
import net.minecraft.nbt.NbtCompound
import net.minecraft.item.Item
import net.minecraft.nbt.NbtElement
import net.minecraft.util.registry.Registry
import us.dison.compactmachines.util.optionFieldOf
import com.mojang.serialization.codecs.RecordCodecBuilder
import us.dison.compactmachines.CompactMachines
import scala.util.chaining.*
import net.minecraft.tag.TagKey
import scala.jdk.CollectionConverters._
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.datafixers.util.Pair
trait ICatalystMatcher {
  def matches(stack : ItemStack) : Boolean 
  def getPossible() : Set[ItemStack]
  def getType() : CatalystType[?]
} 
trait CatalystType[M <: ICatalystMatcher] {
  def getCodec() : Codec[M]
}
class ItemStackCatalystMatcher protected (val item : Item, val nbtMatcher : ItemStack => Boolean, val nbtTag : Option[NbtCompound]) extends ICatalystMatcher {
  private def getNbtTag() = nbtTag
  def matches(stack: ItemStack): Boolean = {
    stack.getItem() == item && nbtMatcher(stack)
  }
  override def getType(): CatalystType[?] = CatalystRegistry.ITEM_STACK_CATALYST
  override def getPossible(): Set[ItemStack] = Set(ItemStack(item).tap(x=> nbtTag.foreach(it => x.setNbt(it))))
}
class ItemTagCatalystMatcher (val tag : TagKey[Item]) extends ICatalystMatcher{
  override def matches(stack: ItemStack): Boolean = {
    if (tag == null)
      true 
    else 
      stack.isIn(tag)
  }
  override def getPossible(): Set[ItemStack] = {
    if (tag == null)
      Set()
    else 
      val tag2 = Registry.ITEM.iterateEntries(tag).asScala
      tag2.map(it => ItemStack(it)).toSet
  }
  override def getType(): CatalystType[?] = CatalystRegistry.ITEM_TAG_CATALYST
}
object ItemTagCatalystMatcher {
  val codec : Codec[ItemTagCatalystMatcher] = RecordCodecBuilder.create(in => in.group(
      TagKey.identifierCodec(Registry.ITEM_KEY).fieldOf("tag").forGetter(_.tag)
  ).apply(in, it => ItemTagCatalystMatcher(it))
  )
}
object ItemStackCatalystMatcher {
  val codec : Codec[ItemStackCatalystMatcher] = RecordCodecBuilder.create(in => in.group(
    Identifier.CODEC.fieldOf("item").forGetter((x : ItemStackCatalystMatcher) => Registry.ITEM.getId(x.item)),
    NbtCompound.CODEC.optionFieldOf("nbt").forGetter((it : ItemStackCatalystMatcher) => it.getNbtTag())
    ).apply(in, ItemStackCatalystMatcher.of))
  def empty = ItemStackCatalystMatcher(null, _ => true, None)
  private def buildMatcher(filter : NbtCompound) : ItemStack => Boolean = {
    if (filter == null)
      stack => true 
    else {
      stack => {
        if (!stack.hasNbt() && !filter.isEmpty()) 
          false 
        else 
          tagMatched(stack.getNbt(), filter)
      }
    }
  }
  private def tagMatched(node : NbtCompound, filter : NbtCompound) : Boolean = {
    if (filter.isEmpty()) 
      true 
    else {
      filter.getKeys().stream().allMatch { key =>
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
  def ofStack(itemStack : ItemStack) = 
    ItemStackCatalystMatcher(itemStack.getItem(), buildMatcher(itemStack.getNbt()), Option(itemStack.getNbt()))
  def of(item : Identifier, nbt : Option[NbtCompound]) = 
    ItemStackCatalystMatcher(Registry.ITEM.get(item), buildMatcher(nbt.orNull), nbt)

}
object CatalystMatcherCodec extends Codec[CatalystType[?]] {
  val matcherCodec : Codec[ICatalystMatcher] = dispatchStable(_.getType(), _.getCodec())
  override def encode[T](input: CatalystType[?], ops: DynamicOps[T], prefix: T): DataResult[T] = {
    val key = CatalystRegistry.CATALYST_TYPES.find {
      case (k, v) => 
        v == input 

    }.map(_._1) 
    key match {
      case Some(k) => 
        val toMerge = ops.createString(key.toString())
        ops.mergeToPrimitive(prefix, toMerge)
      case None => 
        DataResult.error("No such key for catalyst type")
    }
    

  }
  override def decode[T](ops: DynamicOps[T], input: T): DataResult[Pair[CatalystType[?], T]] = {
    Identifier.CODEC.decode(ops, input).flatMap { pair => 
      val id = pair.getFirst()
      if (!CatalystRegistry.CATALYST_TYPES.contains(id)) 
        DataResult.error("Unknown key: " + id)
      else 
        DataResult.success(pair.mapFirst(CatalystRegistry.CATALYST_TYPES.apply))
    }
  }
}
object CatalystRegistry {
  val CATALYST_TYPES = HashMap[Identifier, CatalystType[?]]()
  val ITEM_STACK_CATALYST = new CatalystType[ItemStackCatalystMatcher] {
    def getCodec() = 
      ItemStackCatalystMatcher.codec
  
  }
  val ITEM_TAG_CATALYST = new CatalystType[ItemTagCatalystMatcher] {
    def getCodec() = 
      ItemTagCatalystMatcher.codec
  } 
  CATALYST_TYPES.put(Identifier(CompactMachines.MODID, "item"), ITEM_STACK_CATALYST)
  CATALYST_TYPES.put(Identifier(CompactMachines.MODID, "tag"), ITEM_TAG_CATALYST)
}
