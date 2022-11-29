package us.dison.compactmachines.crafting.recipes.layers

import net.minecraft.block.BlockState
import net.minecraft.util.math.{BlockBox, BlockPos, Box, Vec3d, Vec3i}
import net.minecraft.util.BlockRotation
import us.dison.compactmachines.util.BlockSpaceUtil
import BlockSpaceUtil.&
import us.dison.compactmachines.crafting.recipes.blocks.ComponentPositionLookup
import net.minecraft.world.WorldView
import us.dison.compactmachines.api.crafting.components.IRecipeComponents

import java.util
import java.util.{Optional, stream}
import scala.collection.mutable
import us.dison.compactmachines.*
import us.dison.compactmachines.api.crafting.recipes.layers.IRecipeBlocks

class RecipeBlocks protected(box: BlockBox) extends IRecipeBlocks {
  private var sourceBounds = box
  private var filledBounds = BlockSpaceUtil.getBoundsForBlocks(BlockSpaceUtil.getBlocksIn(sourceBounds))
  private val lookup = ComponentPositionLookup()
  private val states = mutable.Map[BlockPos, BlockState]()
  private val unmatchedStates = mutable.Set[BlockPos]()

  private def copyInfoFrom(og: IRecipeBlocks, offset: BlockPos): Unit = {
    BlockSpaceUtil.getBlocksIn(sourceBounds).foreach { newPos =>
      val key = og.componentAtPosition(newPos)
      key match {
        case Some(k) =>
          lookup.add(newPos.add(offset), k)
        case None =>
          val unidentified = og.stateAtPosition(newPos)
          unidentified match {
            case None => ()
            case Some(s) =>
              if (!s.isAir)
                unmatchedStates += newPos.add(offset).toImmutable
          }

      }
      val oState = og.stateAtPosition(newPos).orNull
      states.update(newPos.add(offset).toImmutable, oState)

    }
    rebuildComponentTotals()
  }

  override def getSourceBounds: BlockBox =
    sourceBounds

  override def allIdentified: Boolean =
    unmatchedStates.isEmpty

  override def getUnmappedPositions: stream.Stream[BlockPos] =
    unmatchedStates.asJava.stream()

  override def getPositionsForComponent(comp: String): stream.Stream[BlockPos] =
    lookup.getPositionsForComponent(comp).asJava.stream()

  override def getFilledBounds: BlockBox = filledBounds

  override def getComponentAtPosition(relative: BlockPos): Optional[String] = {
    lookup.componentsMap.get(relative).toJava
  }

  override def getStateAtPosition(relative: BlockPos): Optional[BlockState] =
    states.get(relative).toJava

  override def getPositions: stream.Stream[BlockPos] =
    states.keySet.asJava.stream()

  override def getNumberKnownComponents: Int =
    lookup.componentTotals.size

  override def rebuildComponentTotals(): Unit =
    lookup.rebuildComponentTotals()

  override def getKnownComponentTotals: util.Map[String, Integer] =
    lookup.componentTotals.map {
      case (k, v) =>
        (k, int2Integer(v))
    }.asJava

  override def slice(box: BlockBox): RecipeBlocks = {
    val blocks = RecipeBlocks(sourceBounds & box)
    blocks.copyInfoFrom(this, BlockPos.ORIGIN)
    blocks
  }

  override def offset(amount: Vec3i): RecipeBlocks = {
    val copy = RecipeBlocks(filledBounds)
    copy.copyInfoFrom(this, BlockPos(amount))
    copy.filledBounds = copy.filledBounds.offset(amount.getX, amount.getY, amount.getZ)
    copy.sourceBounds = copy.sourceBounds.offset(amount.getX, amount.getY, amount.getZ)

    copy
  }
}

object RecipeBlocks {
  def copy(og: IRecipeBlocks): RecipeBlocks =
    val c = RecipeBlocks(og.getSourceBounds)
    c.copyInfoFrom(og, BlockPos.ORIGIN)
    c

  def empty: RecipeBlocks =
    RecipeBlocks(BlockBox(0,0,0, 0, 0, 0))

  def of(bounds: BlockBox, states: Map[BlockPos, BlockState], components: Map[BlockPos, String], unmatchedStates: Set[BlockPos]): RecipeBlocks = {
    val blocks = RecipeBlocks(bounds)
    blocks.states ++= states
    blocks.lookup.componentsMap ++= components
    blocks.unmatchedStates ++= unmatchedStates
    blocks
  }

  def create(blocks: WorldView, components: IRecipeComponents, bounds: BlockBox): RecipeBlocks = {
    val instance = RecipeBlocks(bounds)
    BlockSpaceUtil.getBlocksIn(bounds).map(_.toImmutable).foreach { pos =>

      val state = blocks.getBlockState(pos)

      instance.states.put(pos, state)

      val compKey = components.getKey(state).toScala
      compKey match {
        case Some(ck) =>
          instance.lookup.add(pos, ck)
        case None =>
          instance.unmatchedStates += pos
      }
    }
    instance
  }

  def rotate(og: IRecipeBlocks, rotation: BlockRotation): RecipeBlocks = {
    import us.dison.compactmachines.crafting.recipes.layers._
    if (rotation == BlockRotation.NONE) {
      RecipeBlocks.copy(og)
    } else {
      val originalPositions = og.getPositions.map(_.toImmutable).toScala(List)
      val rotated = BlockSpaceUtil.rotatePositions(originalPositions, rotation)
      val states = mutable.HashMap[BlockPos, BlockState]()
      val componentKeys = mutable.HashMap[BlockPos, String]()
      val unmatchedPositions = mutable.HashSet[BlockPos]()
      originalPositions.foreach { ogPos =>
        val rotatedPos = rotated(ogPos)
        states.put(rotatedPos, og.stateAtPosition(ogPos).orNull)
        og.componentAtPosition(ogPos).foreach(m => componentKeys.put(rotatedPos, m))

      }
      if (!og.allIdentified()) {
        og.unmappedPositions.foreach { pos =>
          val rotatedPos = rotated(pos).toImmutable
          unmatchedPositions += rotatedPos
          og.componentAtPosition(pos).foreach(m => componentKeys(rotatedPos) = m)


        }
      }
      RecipeBlocks.of(og.sourceBounds, states.toMap, componentKeys.toMap, unmatchedPositions.toSet)
    }
  }
}