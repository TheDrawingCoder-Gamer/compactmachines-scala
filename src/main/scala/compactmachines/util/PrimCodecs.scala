// package isn't same as directory? cry about it nerd 
package us.dison.compactmachines.util.codecs

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.PrimitiveCodec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps

object ScalaIntCodec extends PrimitiveCodec[Int]: 
  private val underlying = Codec.INT 
  override def read[T](ops: DynamicOps[T], input: T): DataResult[Int] =
    underlying.read(ops, input).map(Integer2int(_))
  override def write[T](ops: DynamicOps[T], value: Int): T = 
    underlying.write(ops, int2Integer(value))
  override def toString() = 
    "scala.Int"

object ScalaBooleanCodec extends PrimitiveCodec[Boolean]: 
  private val underlying = Codec.BOOL 
  override def read[T](ops: DynamicOps[T], input: T): DataResult[Boolean] = 
    underlying.read(ops, input).map(Boolean2boolean(_))
  override def write[T](ops: DynamicOps[T], value: Boolean): T = 
    underlying.write(ops, boolean2Boolean(value))
  override def toString() = 
    "scala.Boolean"
