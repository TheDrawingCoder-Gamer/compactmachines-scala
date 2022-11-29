package us.dison.compactmachines.crafting.components

import net.minecraft.block.BlockState
import us.dison.compactmachines.api.crafting.components.{IRecipeBlockComponent, IRecipeComponent, IRecipeComponents}
import us.dison.compactmachines.* 

import java.util
import java.util.{Optional, stream}

trait TRecipeComponents extends IRecipeComponents {
  override def getAllComponents: util.Map[String, IRecipeComponent] =
    allComponents.asJava

  def allComponents: Map[String, IRecipeComponent]

  override def getBlockComponents: util.Map[String, IRecipeBlockComponent] =
    blockComponents.asJava

  def blockComponents: Map[String, IRecipeBlockComponent]

  override def getBlock(key: String): Optional[IRecipeBlockComponent] =
    blockFor(key).toJava

  def blockFor(key: String): Option[IRecipeBlockComponent]

  override def getKey(state: BlockState): Optional[String] = keyFor(state).toJava

  def keyFor(state: BlockState): Option[String]

  def getEmptyComponents: stream.Stream[String] =
    emptyComponents.asJavaSeqStream

  def emptyComponents: Iterable[String]
}


