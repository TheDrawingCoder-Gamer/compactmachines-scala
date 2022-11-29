package us.dison.compactmachines.crafting.components

import net.minecraft.block.BlockState
import us.dison.compactmachines.api.crafting.components.{IRecipeBlockComponent, IRecipeComponent}

import scala.collection.mutable


class MiniaturizationRecipeComponents extends TRecipeComponents {
  private val intlblockComponents = mutable.HashMap[String, IRecipeBlockComponent]()
  private val otherComponents = mutable.HashMap[String, IRecipeComponent]()
  override def allComponents: Map[String, IRecipeComponent] = {
    blockComponents ++ otherComponents.toMap
  }
  override def blockComponents: Map[String, IRecipeBlockComponent] =
    intlblockComponents.toMap
  override def isEmptyBlock(key: String): Boolean = {
    intlblockComponents.get(key) match {
      case None => true
      case Some(comp) =>
        comp.isInstanceOf[EmptyBlockComponent]
    }
  }
  override def hasBlock(key: String): Boolean = blockComponents.contains(key)
  override def registerBlock(key: String, component: IRecipeBlockComponent): Unit =
    intlblockComponents.put(key, component)
  override def unregisterBlock(key: String): Unit =
    intlblockComponents.remove(key)
  override def clear(): Unit = {
    intlblockComponents.clear()
    otherComponents.clear()
  }
  override def blockFor(key: String): Option[IRecipeBlockComponent] = {
    blockComponents.get(key)
  }
  override def emptyComponents: Iterable[String] = {
    blockComponents.filter {
      case (k, v) => v.isInstanceOf[EmptyBlockComponent]
    }.keys
  }
  override def keyFor(state: BlockState): Option[String] = {
    blockComponents.find{
      case (k, v) =>
        v.matches(state)
    }.map(_._1)
  }
  override def registerOther(key: String, component: IRecipeComponent): Unit = {
    otherComponents(key) = component
  }
  override def size(): Int =
    otherComponents.size + blockComponents.size
}