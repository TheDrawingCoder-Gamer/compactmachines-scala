package us.dison.compactmachines.crafting.projector

import net.minecraft.util.shape.VoxelShapes
import net.minecraft.util.shape.VoxelShape
import net.minecraft.state.property.DirectionProperty
import net.minecraft.util.math.Direction
import net.minecraft.block.Block
import net.minecraft.block.BlockWithEntity
import net.minecraft.util.StringIdentifiable
import net.minecraft.state.property.EnumProperty
import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockState
import net.minecraft.world.BlockView
import net.minecraft.util.math.BlockPos
import net.minecraft.block.ShapeContext
import net.minecraft.block.BlockEntityProvider
import net.minecraft.state.StateManager.Builder

enum FieldSize(val fieldSize : String, val size : Int, val projectorDistance : Int) extends java.lang.Enum[FieldSize] with StringIdentifiable {
  case Inactive extends FieldSize("inactive", 0, 0)
  case Small extends FieldSize("small", 1, 3)
  case Medium extends FieldSize("medium", 2, 5)
  case Large extends FieldSize("large", 3, 7)
  case Absurd extends FieldSize("absurd", 4, 9)

  def getDimensions = (size * 2) + 1
  def asString() = fieldSize

  def getCenterFromProjector(pos : BlockPos, facing : Direction) = {
    pos.offset(facing, projectorDistance + 1)
  }
}
object ProjectBlock {
  val FACING = DirectionProperty.of("facing", Direction.Type.HORIZONTAL)
  val FIELD_SIZE = EnumProperty.of("field_size", classOf[FieldSize])
  val BASE = VoxelShapes.cuboid(0, 0, 0, 1, 6 / 16d, 1)
  val POLE = VoxelShapes.cuboid(7 / 16d, 6 / 16d, 7 / 16d, 9 / 16d, 14 / 16d, 9 / 16d)
  val SHAPE = VoxelShapes.union(BASE, POLE)
  
  def getDirection(world : BlockView, pos : BlockPos) = {
    val state = world.getBlockState(pos) 
    state.getBlock() match {
      case _ : ProjectBlock => 
        Some(state.get(FACING))
      case _ => 
        None
    }
  }
  def projectorFacesCenter(world : BlockView, pos : BlockPos, actualCenter : BlockPos, size: FieldSize) = {
    getDirection(world, pos).map(projFacing => size.getCenterFromProjector(pos, projFacing) == actualCenter).getOrElse(false)
  }
}

class ProjectBlock(settings : AbstractBlock.Settings) extends Block(settings)  {
  setDefaultState(stateManager.getDefaultState()
    .`with`(ProjectBlock.FACING, Direction.NORTH)
    .`with`(ProjectBlock.FIELD_SIZE, FieldSize.Inactive)
  )
  override def getOutlineShape(state : BlockState, levelReading : BlockView, pos : BlockPos, ctx : ShapeContext) = {
    ProjectBlock.SHAPE
  }
  override def getCullingShape(state : BlockState, worldIn : BlockView, pos : BlockPos) = {
    VoxelShapes.empty()
  }
  override protected def appendProperties(builder: Builder[Block, BlockState]): Unit = {
    super.appendProperties(builder)
    builder.add(ProjectBlock.FACING).add(ProjectBlock.FIELD_SIZE)
  }
  
}
