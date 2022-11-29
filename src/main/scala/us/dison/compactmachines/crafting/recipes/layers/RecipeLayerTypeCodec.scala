package us.dison.compactmachines.crafting.recipes.layers

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.{Codec, DataResult, DynamicOps}
import net.minecraft.util.Identifier
import us.dison.compactmachines.api.crafting.recipes.layers.RecipeLayerType
import us.dison.compactmachines.crafting.Registrar

object RecipeLayerTypeCodec extends Codec[RecipeLayerType[?]] {
  def decode[T](ops: DynamicOps[T], input: T): DataResult[Pair[RecipeLayerType[_], T]] = {
    Identifier.CODEC.decode(ops, input).flatMap { keyValuePair =>
      val id = keyValuePair.getFirst
      val reg = Registrar.layerTypeRegistry
      if (!reg.contains(id)) {
      }
      reg.get(id) match {
        case None =>
          DataResult.error(s"Unknown registry key: $id")
        case Some(i) =>
          DataResult.success(keyValuePair.mapFirst(_ => i))
      }
    }
  }

  def encode[T](input: RecipeLayerType[?], ops: DynamicOps[T], prefix: T): DataResult[T] = {
    val reg = Registrar.layerTypeRegistry
    val key = reg.find {
      case (k, v) => v == input
    }.map(_._1)
    key match {
      case None => DataResult.error(s"Unknown registry element $input")
      case Some(k) =>
        val toMerge = ops.createString(key.toString)
        ops.mergeToPrimitive(prefix, toMerge)
    }
  }
}
