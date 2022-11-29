package us.dison.compactmachines.crafting

import us.dison.compactmachines.api.crafting.IRegistrar
import us.dison.compactmachines.api.crafting.recipes.layers.RecipeLayerType
import net.minecraft.util.Identifier
import us.dison.compactmachines.api.crafting.catalyst.CatalystType
import us.dison.compactmachines.api.crafting.components.RecipeComponentType

import scala.collection.mutable
object Registrar extends IRegistrar {
  protected[compactmachines] val layerTypeRegistry: mutable.Map[Identifier, RecipeLayerType[_]] = mutable.HashMap[Identifier, RecipeLayerType[?]]()
  protected [compactmachines] val componentTypeRegistry : mutable.Map[Identifier, RecipeComponentType[_]] = mutable.HashMap()
  protected[compactmachines] val catalystTypeRegistry : mutable.Map[Identifier, CatalystType[_]] = mutable.HashMap()
  override def registerLayerType(name : Identifier, layerType : RecipeLayerType[?]): Unit = layerTypeRegistry.update(name, layerType)

  override def registerComponentType(id: Identifier, componentType: RecipeComponentType[_]): Unit = componentTypeRegistry.update(id, componentType)

  override def registerCatalystType(id: Identifier, catalystType: CatalystType[_]): Unit = catalystTypeRegistry.update(id, catalystType)

}
