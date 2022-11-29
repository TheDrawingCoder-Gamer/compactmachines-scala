package us.dison.compactmachines.crafting.components

import net.minecraft.block.BlockState
import us.dison.compactmachines.api.crafting.components.{IRecipeBlockComponent, IRecipeComponent, IRecipeComponents}
import us.dison.compactmachines.*

object IRecipeComponentsExt {
  extension (rc: IRecipeComponents) {
    def allComponents: Map[String, IRecipeComponent] =
      rc.getAllComponents.asScala.toMap
    def blockComponents: Map[String, IRecipeBlockComponent] =
      rc.getBlockComponents.asScala.toMap
    def blockFor(key: String): Option[IRecipeBlockComponent] =
      rc.getBlock(key).toScala
    def keyFor(state: BlockState) =
      rc.getKey(state).toScala
    def emptyComponents =
      rc.getEmptyComponents.toScala(List)
  }
}
