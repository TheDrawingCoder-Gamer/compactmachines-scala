package us.dison.compactmachines.crafting.components

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.{Codec, DataResult, DynamicOps}
import net.minecraft.util.Identifier
import us.dison.compactmachines.CompactMachinesCraftingPlugin
import us.dison.compactmachines.api.crafting.components.RecipeComponentType
import us.dison.compactmachines.crafting.Registrar
import us.dison.compactmachines.util.givens.given
import scala.language.implicitConversions

object RecipeComponentTypeCodec extends Codec[RecipeComponentType[?]] {
  override def decode[T](ops: DynamicOps[T], input: T): DataResult[Pair[RecipeComponentType[?], T]] = {
    Identifier.CODEC.decode(ops, input).flatMap { rl =>
      val resource = rl.getFirst
      val res = rl.getSecond
      Registrar.componentTypeRegistry.get(resource) match {
        case Some(value) =>
          DataResult.success((value, res))
        case None =>
          DataResult.error(s"Unknown registry key $resource")
      }
    }
  }

  override def encode[T](input: RecipeComponentType[?], ops: DynamicOps[T], prefix: T): DataResult[T] = {
    val key = Registrar.componentTypeRegistry.view.find {
      case (_, v) => v == input
    }.map(_._1)
    key match {
      case Some(daKey) =>
        val tomerge = ops.createString(daKey.toString)
        ops.mergeToPrimitive(prefix, tomerge)
      case None =>
        DataResult.error(s"Unknown registry element $input")
    }
  }
}
