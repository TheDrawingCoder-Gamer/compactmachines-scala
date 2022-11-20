package us.dison.compactmachines.crafting

import us.dison.compactmachines.api.crafting.IRegistrar
import us.dison.compactmachines.api.crafting.recipes.layers.RecipeLayerType
import net.minecraft.util.Identifier
import scala.collection.mutable 
object Registrar extends IRegistrar {
  protected[compactmachines] val registry = mutable.HashMap[Identifier, RecipeLayerType[?]]()
  override def registerLayerType(name : Identifier, layerType : RecipeLayerType[?]) = 
    registry.update(name, layerType)
}
