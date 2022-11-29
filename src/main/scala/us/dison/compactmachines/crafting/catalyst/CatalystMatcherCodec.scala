package us.dison.compactmachines.crafting.catalyst

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.{Codec, DataResult, DynamicOps}
import net.minecraft.util.Identifier
import us.dison.compactmachines.api.crafting.catalyst.{CatalystType, ICatalystMatcher}
import us.dison.compactmachines.crafting.Registrar
import us.dison.compactmachines.util.givens.given
import scala.language.implicitConversions

object CatalystMatcherCodec extends Codec[CatalystType[?]] {
  val matcherCodec: Codec[ICatalystMatcher] = dispatchStable(_.getType(), _.getCodec())

  override def encode[T](input: CatalystType[?], ops: DynamicOps[T], prefix: T): DataResult[T] = {
    val key = Registrar.catalystTypeRegistry.find {
      case (k, v) =>
        v == input

    }.map(_._1)
    key match {
      case Some(k) =>
        val toMerge = ops.createString(key.toString)
        ops.mergeToPrimitive(prefix, toMerge)
      case None =>
        DataResult.error("No such key for catalyst type")
    }


  }

  override def decode[T](ops: DynamicOps[T], input: T): DataResult[Pair[CatalystType[?], T]] = {
    Identifier.CODEC.decode(ops, input).flatMap { rl =>
      val id = rl.getFirst
      val res = rl.getSecond
      if (!Registrar.catalystTypeRegistry.contains(id))
        DataResult.error(s"Unknown key: $id")
      else
        DataResult.success((Registrar.catalystTypeRegistry(id), res))
    }
  }
}
