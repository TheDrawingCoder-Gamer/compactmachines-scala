package us.dison.compactmachines.crafting.recipes.layers

import net.minecraft.util.math.{BlockBox, BlockPos, Box, Vec3d}
import us.dison.compactmachines.api.crafting.components.IRecipeComponents
import us.dison.compactmachines.api.crafting.recipes.layers.{IRecipeBlocks, ISymmetricLayer, RecipeLayerType}
import us.dison.compactmachines.crafting.recipes.layers.dim.IDynamicSizedRecipeLayer
import us.dison.compactmachines.util.BlockSpaceUtil
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

import java.util
import scala.collection.mutable
import us.dison.compactmachines.*

class HollowComponentRecipeLayer(val component: String) extends TRecipeLayer with IDynamicSizedRecipeLayer with ISymmetricLayer {
  private val filledPositions = mutable.Set.empty[BlockPos]
  private var recipeDimensions : BlockBox = BlockBox(0,0,0, 0, 0, 0)

  def dimensions : BlockBox = recipeDimensions
  def dimensions_=(box : BlockBox) : Unit = recipeDimensions = box
  override def getComponents: util.Set[String] = Set(component).asJava

  override def componentTotals: Map[String, Int] = Map((component, getNumberFilledPositions))

  override def componentAtPosition(pos: BlockPos): Option[String] = {
    if (filledPositions.contains(pos))
      Option(component)
    else
      None
  }

  override def positionsForComponent(comp: String): List[BlockPos] =
    if (comp == component)
      filledPositions.toList
    else
      List()

  override def matches(components: IRecipeComponents, blocks: IRecipeBlocks): Boolean = {
    if (!blocks.allIdentified()) {
      val anyNonAir = blocks.unmappedPositions.flatMap(blocks.stateAtPosition).exists(!_.isAir)
      if (anyNonAir) return false
    }
    val totalsInWorld = blocks.knownComponentTotals
    // TODO: use match
    if (!totalsInWorld.contains(component)) {
      false
    } else {
      if (totalsInWorld.size > 1) {
        val everythingElseEmpty = totalsInWorld.keySet.filter(c => c != component).forall(components.isEmptyBlock)
        if (!everythingElseEmpty) return false
      }
      val targetCount = totalsInWorld(component)
      val layerCount = filledPositions.size

      layerCount == targetCount
    }
  }
  

  override def recalculateRequirements(): Unit = {
    this.filledPositions.clear()
    this.filledPositions.addAll(BlockSpaceUtil.getWallPositions(recipeDimensions).toSet)
  }

  override def getType: RecipeLayerType[_] = HollowComponentRecipeLayer

  def getNumberFilledPositions: Int = filledPositions.size
}

object HollowComponentRecipeLayer extends RecipeLayerType[HollowComponentRecipeLayer] {
  override val getCodec: Codec[HollowComponentRecipeLayer] = RecordCodecBuilder.create(it => it.group(
    Codec.STRING.fieldOf("component").forGetter(_.component)
  ).apply(it, i => HollowComponentRecipeLayer(i)))
}