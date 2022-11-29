package us.dison.compactmachines.util

import com.mojang.serialization.*

import java.util.Objects

class OptionFieldCodec[A](private val name: String, private val elementCodec: Codec[A]) extends MapCodec[Option[A]] {
  override def decode[T](ops: DynamicOps[T], input: MapLike[T]): DataResult[Option[A]] = {
    val value = input.get(name)
    if (value == null)
      DataResult.success(None)
    else {
      val parsed = elementCodec.parse(ops, value)
      if (parsed.result().isPresent)
        parsed.map(Some.apply)
      else
        DataResult.success(None)
    }
  }

  override def encode[T](input: Option[A], ops: DynamicOps[T], prefix: RecordBuilder[T]): RecordBuilder[T] = {
    input match {
      case Some(i) => prefix.add(name, elementCodec.encodeStart(ops, i))
      case None => prefix
    }
  }

  override def keys[T](ops: DynamicOps[T]): java.util.stream.Stream[T] =
    java.util.stream.Stream.of(ops.createString(name))

  override def equals(o: Any): Boolean =
    if (this.eq(o.asInstanceOf[Object]))
      true
    else if (o == null || this.getClass != o.getClass)
      false
    else {
      val that: OptionFieldCodec[?] = o.asInstanceOf[OptionFieldCodec[?]]
      this.name == that.name && this.elementCodec == that.elementCodec
    }

  override def hashCode(): Int = Objects.hash(name, elementCodec)

  override def toString = s"OptionFieldCodec[$name : $elementCodec]"
}
