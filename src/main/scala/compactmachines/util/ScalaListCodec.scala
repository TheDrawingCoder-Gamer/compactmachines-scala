package us.dison.compactmachines.util 

import com.mojang.serialization.codecs.ListCodec
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import scala.jdk.CollectionConverters._
import com.mojang.datafixers.util.Pair
import com.mojang.serialization.MapCodec
class ScalaListCodec[A](private val elementCodec: Codec[A]) extends Codec[List[A]]: 
  private val underlying = ListCodec(elementCodec)
  override def encode[T](input: List[A], ops: DynamicOps[T], prefix: T): DataResult[T] = 
    underlying.encode(input.asJava, ops, prefix)
  override def decode[T](ops: DynamicOps[T], input: T): DataResult[Pair[List[A], T]] = 
    underlying.decode(ops, input).map(_.mapFirst((ls: java.util.List[A]) => List.from(ls.asScala : scala.collection.mutable.Buffer[A])))
  // this is defo wrong
  override def equals(x: Any): Boolean = 
    super.equals(x) || underlying.equals(x)
  override def hashCode(): Int = 
    underlying.hashCode()
  override def toString(): String = 
    s"ScalaListCodec[$elementCodec]"

class ScalaMapCodec[K,V](private val keyCodec: Codec[K], private val valueCodec : Codec[V]) extends Codec[Map[K,V]] {
  private val underlying = Codec.unboundedMap(keyCodec, valueCodec)
  override def encode[T](input: Map[K, V], ops : DynamicOps[T], prefix : T) : DataResult[T] = {
    underlying.encode(input.asJava, ops, prefix)
  }
  override def decode[T](ops : DynamicOps[T], input : T) : DataResult[Pair[Map[K,V], T]] = {
    underlying.decode(ops, input).map(_.mapFirst((mp : java.util.Map[K, V]) => mp.asScala.toMap))
  }
  override def equals(x: Any): Boolean = 
    super.equals(x) || underlying.equals(x)
  override def hashCode() = 
    underlying.hashCode()
  override def toString() : String = 
    s"ScalaMapCoded[$keyCodec, $valueCodec]"
}
