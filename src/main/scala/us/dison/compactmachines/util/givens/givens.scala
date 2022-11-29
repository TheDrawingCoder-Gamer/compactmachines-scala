package us.dison.compactmachines.util.givens

import net.minecraft.util.math.{BlockPos, Direction, Vec3i}
import net.minecraft.block.BlockState
import Direction.Axis
import com.mojang.datafixers.util.Pair
import net.minecraft.state.property.Property
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.MapLike
import com.mojang.serialization.RecordBuilder
import net.minecraft.world.World

import java.util.Objects
import us.dison.compactmachines.util.OptionFieldCodec
import net.minecraft.block.Block
import us.dison.compactmachines.api.crafting.catalyst.{CatalystType, ICatalystMatcher}
import us.dison.compactmachines.api.crafting.components.{IRecipeComponent, RecipeComponentType}
import us.dison.compactmachines.api.crafting.recipes.layers.{IRecipeLayer, RecipeLayerType}
object axis {
  extension (axis: Axis) {
    def crossAxis = axis match {
      case Axis.X => Axis.Z
      case Axis.Z => Axis.X
      case Axis.Y => Axis.Y
    }
  }
}

object direction {
  extension (dir : Direction) {
    def opposite = dir.getOpposite
    def axis = dir.getAxis
  }
}
object blockState {
  extension (state: BlockState) {
    def copy[T <: Comparable[T]](prop: Property[T], value: T) : BlockState = {
      state.`with`[T, T](prop, value)
    }
    def block: Block = state.getBlock

  }
}

object world {
  extension (world : World) {
    def registryKey  = world.getRegistryKey
  }
}



object blockPos {
  extension (pos : Vec3i) {
    def x = pos.getX
    def y = pos.getY
    def z = pos.getZ

  }
  extension (pos : BlockPos) {
    def copy(x : Int = pos.x, y : Int = pos.y, z :Int = pos.z) : BlockPos =
      BlockPos(x, y, z)
  }
}


object codec {
  extension[T] (codec: Codec[T]) {
    def optionFieldOf(name: String): MapCodec[Option[T]] =
      OptionFieldCodec[T](name, codec)
    def optionFieldOf(name: String, defaultValue: T): MapCodec[T] = {
      optionFieldOf(name).xmap(o => o.getOrElse(defaultValue), a => if (a == defaultValue) None else Some(a))
    }
  }
}

given codecCatalyst[A <: ICatalystMatcher]: Conversion[Codec[A], CatalystType[A]] with {
  def apply(codec : Codec[A]) : CatalystType[A] = {
    () => codec
  }
}

given codecRecipeLayer[A <: IRecipeLayer]: Conversion[Codec[A], RecipeLayerType[A]] = () => _

given codecRecipeComponent[A <: IRecipeComponent] : Conversion[Codec[A], RecipeComponentType[A]] = () => _

given tuplePair[A, B] : Conversion[Pair[A, B], (A, B)] = it => (it.getFirst, it.getSecond)
given pairTuple[A, B] : Conversion[(A, B), Pair[A, B]] = it => Pair(it._1, it._2)