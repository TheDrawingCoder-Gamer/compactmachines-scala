package us.dison.compactmachines.util

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction 
import net.minecraft.block.BlockState
import Direction.Axis
import net.minecraft.state.property.Property
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.MapLike
import com.mojang.serialization.RecordBuilder
import java.util.Objects
extension (axis : Axis) {
  def crossAxis = axis match {
    case Axis.X => Axis.Z 
    case Axis.Z => Axis.X 
    case Axis.Y => Axis.Y
  } 
}
extension (state : BlockState) {
  def copy[T <: Comparable[T]](prop : Property[T], value : T) = {
    state.`with`(prop, value)
  }
}
class OptionFieldCodec[A](private val name : String, private val elementCodec : Codec[A]) extends MapCodec[Option[A]] {
  override def decode[T](ops: DynamicOps[T], input: MapLike[T]): DataResult[Option[A]] = {
    val value = input.get(name)
    if (value == null) 
      DataResult.success(None)
    else {
      val parsed = elementCodec.parse(ops, value)
      if (parsed.result().isPresent()) 
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
  override def keys[T](ops : DynamicOps[T]): java.util.stream.Stream[T] = 
    java.util.stream.Stream.of(ops.createString(name))
  override def equals(o: Any): Boolean = 
    if (this.eq(o.asInstanceOf[Object]))
      true 
    else if (o == null || this.getClass() != o.getClass())
      false 
    else {
      val that : OptionFieldCodec[?] = o.asInstanceOf[OptionFieldCodec[?]]
      this.name == that.name && this.elementCodec == that.elementCodec
    }
  override def hashCode(): Int = Objects.hash(name, elementCodec)
  override def toString() = s"OptionFieldCodec[ ${name} : ${elementCodec}]"
}
extension [T](codec : Codec[T]) {
  def optionFieldOf(name : String) : MapCodec[Option[T]] = {
    OptionFieldCodec[T](name, codec)
  }
  def optionFieldOf(name : String, defaultValue : T) : MapCodec[T] = {
    optionFieldOf(name).xmap(o => o.getOrElse(defaultValue), a => if (a == defaultValue) None else Some(a))
  }
}
