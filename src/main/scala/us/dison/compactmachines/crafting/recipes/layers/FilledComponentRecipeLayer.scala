package us.dison.compactmachines.crafting.recipes.layers

import net.minecraft.util.math.{BlockBox, BlockPos, Box, Vec3d}
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import us.dison.compactmachines.CompactMachines

import scala.jdk.CollectionConverters.*
import us.dison.compactmachines.api.crafting.components.IRecipeComponents
import us.dison.compactmachines.api.crafting.recipes.layers.{IRecipeBlocks, ISymmetricLayer, RecipeLayerType}
import us.dison.compactmachines.crafting.recipes.layers.dim.IDynamicSizedRecipeLayer
import us.dison.compactmachines.util.BlockSpaceUtil
import us.dison.compactmachines.crafting.recipes.layers.givens.ccRecipeBlocks.*

import java.util

class FilledComponentRecipeLayer(val component: String = "-") extends TRecipeLayer with IDynamicSizedRecipeLayer with ISymmetricLayer {
  // i frankly don't care about encapsulation
  var recipeDimensions: BlockBox = BlockBox(0, 0, 0, 0, 0, 0)

  override def getComponents: util.Set[String] = {
    Set(component).asJava
  }
  override def dimensions : BlockBox = recipeDimensions

  def getNumberFilledPositions: Int = {
    Option(recipeDimensions) match {
      case Some(dim) =>
        Math.ceil(dim.getBlockCountX * dim.getBlockCountZ).toInt
      case None =>
        0
    }
  }

  override def componentTotals: Map[String, Int] = Map[String, Int]((component, getNumberFilledPositions))

  override def componentAtPosition(pos: BlockPos): Option[String] = {
    if (recipeDimensions.contains(BlockPos(pos.getX, pos.getY, pos.getZ)))
      Option(component)
    else
      None
  }

  override def positionsForComponent(componentKey: String): Seq[BlockPos] = {
    if (component != componentKey) {
      List()
    } else {
      BlockSpaceUtil.getBlocksIn(recipeDimensions)
    }
  }

  override def matches(components: IRecipeComponents, blocks: IRecipeBlocks): Boolean = {
    if (!blocks.allIdentified())
      false
    else {
      val totalsInWorld = blocks.knownComponentTotals
      if (totalsInWorld.size != 1) {
        CompactMachines.LOGGER.info("world has more than 1 component")
        false
      } else {
        totalsInWorld.get(component).contains(getNumberFilledPositions)
      }
    }
  }

  override def getType: RecipeLayerType[_] = FilledComponentRecipeLayer

  override def dimensions_=(box: BlockBox): Unit = this.recipeDimensions = box

}

object FilledComponentRecipeLayer extends RecipeLayerType[FilledComponentRecipeLayer] {
  val codec: Codec[FilledComponentRecipeLayer] = RecordCodecBuilder.create(in => in.group(Codec.STRING.fieldOf("component").forGetter((it: FilledComponentRecipeLayer) => it.component)).apply(in, it => FilledComponentRecipeLayer.apply(it)))

  def getCodec: Codec[FilledComponentRecipeLayer] = codec
}