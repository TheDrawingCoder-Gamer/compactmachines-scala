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

import scala.jdk.CollectionConverters.*
import scala.jdk.StreamConverters.*
import us.dison.compactmachines.util
import util.Ticker
import util.givens.blockState.*
import util.givens.blockPos.*
import util.givens.direction.*
import util.givens.axis.*

import scala.math.Ordering.Implicits.infixOrderingOps
import net.minecraft.item.ItemPlacementContext
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.Hand
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.world.World
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.Camera
import net.minecraft.client.world.ClientWorld
import net.minecraft.client.MinecraftClient
import net.minecraft.client.color.block.BlockColors
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.model.BakedQuad
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.MathHelper
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.OverlayTexture

import scala.collection.mutable
import us.dison.compactmachines.client.CCRenderTypes
import net.minecraft.block.Blocks
import com.mojang.blaze3d.systems.RenderSystem
import us.dison.compactmachines.CompactMachines
import net.minecraft.state.property.BooleanProperty
import net.minecraft.client.render.RenderLayer
import us.dison.compactmachines.api.crafting.field.FieldSize
import FieldSizeExt.*
import net.minecraft.item.ItemStack
import net.minecraft.entity.LivingEntity
import net.minecraft.world
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity
import us.dison.compactmachines.crafting.field.*
import net.minecraft.server.world.ServerWorld
import us.dison.compactmachines.api.crafting.field.IMiniaturizationField
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType

import scala.util.chaining.*
import net.minecraft.item

import scala.annotation.nowarn
object FieldSizeExt {
  extension (fs : FieldSize) {
    def fieldSize = fs.name()
    def projectorPoses = fs.getProjectorPoses.toScala(List)
    def projectorPosesAt(pos : BlockPos) = fs.getProjectorPosesAt(pos).toScala(List)
    def originCenter = fs.getOriginCenter
    def projectorPosesAxis(center : BlockPos, axis : Direction.Axis) = fs.getProjectorPosesOnAxis(center, axis).toScala(List)
  }
}
/*
enum FieldSize(val fieldSize : String, val size : Int, val projectorDistance : Int) 
  extends java.lang.Enum[FieldSize] 
  with StringIdentifiable {
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
  def getProjLocationWithDirection(center : BlockPos, facing : Direction) = 
    center.offset(facing, projectorDistance + 1)
  // toImmutable OR ELSE
  val originCenter = BlockPos.ORIGIN.toImmutable().add(size, size, size).toImmutable()

  def projectorPoses : List[BlockPos] = 
    List(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)
      .map( it => getProjLocationWithDirection(originCenter, it))

  def projectorPosesAt(pos : BlockPos) = 
    projectorPoses.map(it => it.add(pos.getX(), pos.getY(), pos.getZ()))
  def getOppositeProjector(pos : BlockPos, facing : Direction) = {
    val center = getCenterFromProjector(pos, facing)
    getProjLocationWithDirection(center, facing)
  }
  def projectorPosesAxis(center : BlockPos, axis : Direction.Axis) = {
    axis match {
      case Direction.Axis.Y => List() 
      case _ => 
        Direction.values().filter(axis.test).toList
          .map(it => getProjLocationWithDirection(center, it))
    }
  }

}

object FieldSize {

  def validSizes = List(Small, Medium, Large, Absurd)
  def canFitDimensions(i : Int) = 
    i <= Absurd.getDimensions && i >= Small.getDimensions
}
*/ 

object ProjectorBlock {
  val FACING: DirectionProperty = DirectionProperty.of("facing", Direction.Type.HORIZONTAL)
  val FIELD_SIZE : EnumProperty[FieldSize] = EnumProperty.of("field_size", classOf[FieldSize])
  val BASE: VoxelShape = VoxelShapes.cuboid(0, 0, 0, 1, 6 / 16d, 1)
  val POLE: VoxelShape = VoxelShapes.cuboid(7 / 16d, 6 / 16d, 7 / 16d, 9 / 16d, 14 / 16d, 9 / 16d)
  val SHAPE: VoxelShape = VoxelShapes.union(BASE, POLE)
  val SPOOKY: BooleanProperty = BooleanProperty.of("spooky")
  def getDirection(world : BlockView, pos : BlockPos): Option[Direction] = {
    val state = world.getBlockState(pos)
    state.getBlock match {
      case _ : ProjectorBlock =>
        Some(state.get(FACING))
      case _ =>
        None
    }
  }
  def projectorFacesCenter(world : BlockView, pos : BlockPos, actualCenter : BlockPos, size: FieldSize): Boolean = {
    // exists == .map(???).getOrElse(false)
    getDirection(world, pos).exists(projFacing => size.getCenterFromProjector(pos, projFacing) == actualCenter)
  }
  def getOpposite(world : BlockView, proj : BlockPos, size : FieldSize): Option[BlockPos] = {
    getDirection(world, proj).map(it => size.getOppositeProjector(proj, it))
  }
  def hasProjectorOpposite(world : BlockView, initial : BlockPos, size : FieldSize, initialFacing : Direction): Boolean = {
    val oppositePos = size.getOppositeProjector(initial, initialFacing)
    val oppositeState = world.getBlockState(oppositePos)
    oppositeState.block match {
      case _ : ProjectorBlock =>
        val oppFacing = oppositeState.get(FACING)
        oppFacing == initialFacing.opposite
      case _ =>
        false
    }
  }
  def hasValidCross(world : BlockView, initial : BlockPos, size : FieldSize, initialFacing : Direction): Boolean = {
    val center = size.getCenterFromProjector(initial, initialFacing)
    val crossAxis = initialFacing.axis.crossAxis
    val crossAxesPos = size.projectorPosesAxis(center, crossAxis)
    crossAxesPos.exists { pos  =>
      projectorFacesCenter(world, pos, center, size)
    }
  }
  def getSmallestOpposite(world : BlockView, initial : BlockPos, facing : Direction): Option[FieldSize] = {
    FieldSize.VALID_SIZES.find( it => hasProjectorOpposite(world, initial, it, facing))
  }
  def getSmallestCross(world : BlockView, initial : BlockPos, facing : Direction): Option[FieldSize] = {
    FieldSize.VALID_SIZES.find( it => hasValidCross(world, initial, it, facing))
  }

  def getFieldCenter(state : BlockState, projector : BlockPos): BlockPos =
    state.get(FIELD_SIZE).getCenterFromProjector(projector, state.get(FACING))
  def getSmallest(world : BlockView, initial : BlockPos, facing : Direction): Option[FieldSize] = {
    val smallOpp = getSmallestOpposite(world, initial, facing)
    val smallCross = getSmallestCross(world, initial, facing)
    (smallOpp, smallCross) match {
      case (Some(a), Some(b)) =>
        // min
        Some(if (a.compareTo(b) <= 0) a else b )
      case _ =>
        smallOpp.orElse(smallCross)
    }
  }
  def getMissingProjectorsSize(world : BlockView, size : FieldSize, center : BlockPos): Seq[BlockPos] = {
    size.projectorPosesAt(center).filterNot(it => projectorFacesCenter(world, it, center, size))
  }
  def getMissingProjectors(world : BlockView, proj : BlockPos, dir : Direction): Seq[BlockPos] = {
    getSmallest(world, proj, dir).map { size =>
      val center = size.getCenterFromProjector(proj, dir)
      getMissingProjectorsSize(world, size, center)
    }.getOrElse(getValidOppositePositions(proj, dir))

  }
  def getValidOppositePositions(pos : BlockPos, facing : Direction): Seq[BlockPos] = {
    FieldSize.VALID_SIZES.map(it => it.getOppositeProjector(pos, facing)).toList
  }
  def isActive(state : BlockState): Boolean = {
    state.get[FieldSize](FIELD_SIZE) != FieldSize.INACTIVE
  }
  def activateProjector(level : World, pos : BlockPos, fieldSize : FieldSize): Unit = {
    if (level.isChunkLoaded(pos)) {
      val state =level.getBlockState(pos)
      state.getBlock match {
        case _ : ProjectorBlock =>
          if (state.get(FIELD_SIZE) != fieldSize) {
            val newState = state.`with`(FIELD_SIZE, fieldSize)
            level.setBlockState(pos, newState, Block.NOTIFY_ALL)
          }
        case _ => ()
      }

    }
  }
  def deactivateProjector(level : World, pos : BlockPos): Unit = {
    val curState = level.getBlockState(pos)
    curState.block match {
      case _ : ProjectorBlock =>
        val newState = curState.`with`(FIELD_SIZE, FieldSize.INACTIVE)
        level.setBlockState(pos, newState, Block.NOTIFY_ALL)
      case _ => ()
    }
  }
}

//noinspection ScalaDeprecation
class ProjectorBlock(settings : AbstractBlock.Settings) extends BlockWithEntity(settings) {
  setDefaultState(stateManager.getDefaultState
    .`with`(ProjectorBlock.FACING, Direction.NORTH)
    .`with`[FieldSize, FieldSize](ProjectorBlock.FIELD_SIZE, FieldSize.INACTIVE)
    // .`with`(ProjectBlock.SPOOKY, false)
  )
  @nowarn("cat=deprecation")
  @SuppressWarnings("deprecation")
  override def getOutlineShape(state : BlockState, levelReading : BlockView, pos : BlockPos, ctx : ShapeContext): VoxelShape = {
    ProjectorBlock.SHAPE
  }
  @SuppressWarnings("deprecation")
  override def getCullingShape(state : BlockState, worldIn : BlockView, pos : BlockPos): VoxelShape = {
    VoxelShapes.empty()
  }

  override protected def appendProperties(builder: Builder[Block, BlockState]): Unit = {
    super.appendProperties(builder)
    builder.add(ProjectorBlock.FACING).add(ProjectorBlock.FIELD_SIZE)
  }

  override def getPlacementState(ctx: ItemPlacementContext): BlockState = {
    val looking : Direction = Option(ctx.getPlayer).map(it => if it.isSneaking then ctx.getPlayerFacing.getOpposite else ctx.getPlayerFacing).getOrElse(ctx.getPlayerFacing)
    val pos = ctx.getBlockPos
    val missing = ProjectorBlock.getMissingProjectors(ctx.getWorld, pos, looking)
    val hasMissing = missing.exists(_ != pos)
    val size =
      if (!hasMissing) {
        ProjectorBlock.getSmallest(ctx.getWorld, pos, looking).getOrElse(FieldSize.INACTIVE)
      } else {
        FieldSize.INACTIVE
      }


    getDefaultState.`with`(ProjectorBlock.FACING, looking).`with`(ProjectorBlock.FIELD_SIZE, size)
  }
  @annotation.nowarn("cat=deprecation")
  @SuppressWarnings("deprecation")
  override def onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult = {
    if (world.isClient) {
      val hasMissing = ProjectorBlock.getMissingProjectors(world, pos, state.get(ProjectorBlock.FACING)).nonEmpty
      if (hasMissing) {
        HologramRenderer.resetRenderTime()
        HologramRenderer.setProjector(world, pos)
      }
    }
    ActionResult.SUCCESS
  }

  @SuppressWarnings("deprecation")
  override def onBlockAdded(state: BlockState, world: net.minecraft.world.World, pos: BlockPos, oldState: BlockState, b : Boolean): Unit = {
    val missing = ProjectorBlock.getMissingProjectors(world, pos, state.get(ProjectorBlock.FACING))
    val hasMissing = missing.exists(_ != pos)
    val size : FieldSize = state.get(ProjectorBlock.FIELD_SIZE)
    val fieldcenter = size.getCenterFromProjector(pos, state.get(ProjectorBlock.FACING))

    if (size == FieldSize.INACTIVE) return
    if (!hasMissing) {
      val server = world.getServer
      if (server == null) return
      if (world.isRegionLoaded(fieldcenter.x, fieldcenter.y, fieldcenter.z + size.getProjectorDistance, fieldcenter.z + size.getProjectorDistance)) {
        size.getProjectorPosesAt(fieldcenter).forEach( proj => ProjectorBlock.activateProjector(world, proj, size))


        val center = ProjectorBlock.getFieldCenter(state, pos)

        val fields = ActiveWorldFields.getOrCreate(world)
        if (!fields.hasActiveField(center)) {
          val field = fields.registerField(MiniaturizationField.ofSizeAndCenter(size,center))
          field.fieldContentsChanged()

          // TODO: Networking
        }

      }
    } else {
      world.setBlockState(pos, state.`with`(ProjectorBlock.FIELD_SIZE, FieldSize.INACTIVE), Block.NOTIFY_ALL)
    }
  }
  @SuppressWarnings("deprecation")
  override def onStateReplaced(state: BlockState, world: net.minecraft.world.World, pos: BlockPos, newState: BlockState, moved: Boolean): Unit = {
    val center = ProjectorBlock.getFieldCenter(state, pos)
    val size = state.get(ProjectorBlock.FIELD_SIZE)

    if (ProjectorBlock.isActive(state)) {
      size.getProjectorPosesAt(center).forEach(proj => ProjectorBlock.deactivateProjector(world, proj))

      if (!world.isClient) {
        val serverWorld = world.asInstanceOf[ServerWorld]
        val fields = ActiveWorldFields.getOrCreate(serverWorld)
        if (fields.hasActiveField(center)) {
          val field = fields.get(center)
          field match {
            case Some(fl) =>
              if (fl.isEnabled) {
                fields.unregisterField(center)
                fl.handleDestabilize()
                fl.cleanForDisposal()
              }
            case None => ()
          }
        }
      }
    }
  }

  override def createBlockEntity(pos: BlockPos, state: BlockState): net.minecraft.block.entity.BlockEntity = {
    val facing = state.get(ProjectorBlock.FACING)
    // North projector is the ticker
    ProjectorBlockEntity(pos, state, facing == Direction.SOUTH)
  }
  @SuppressWarnings("deprecation")
  override def neighborUpdate(state: BlockState, world: net.minecraft.world.World, pos: BlockPos, block: Block, fromPos: BlockPos, notify: Boolean): Unit = {
    super.neighborUpdate(state, world, pos, block, fromPos, notify)

    if (world.isClient) return

    if (ProjectorBlock.isActive(state)) {
      val tile = world.getBlockEntity(pos)
      tile match {
        case fpt : ProjectorBlockEntity =>
          if (world.getReceivedRedstonePower(pos) > 0) {
            fpt.fieldRef.foreach(_.disable())

          } else {
            fpt.fieldRef.foreach(_.checkRedstone())
          }
      }
    } else {
      ProjectorBlock.getSmallestOpposite(world, pos, state.get(ProjectorBlock.FACING)).foreach { size =>
        val center : BlockPos = size.getCenterFromProjector(pos, state.get(ProjectorBlock.FACING))
        val fields = ActiveWorldFields.getOrCreate(world)
        fields.get(center).foreach(_.checkRedstone)

      }
    }
  }
  // override def getTicker[T <: net.minecraft.block.entity.BlockEntity](world: net.minecraft.world.World, state: BlockState, kind: BlockEntityType[T]): BlockEntityTicker[T] =
  //   BlockWithEntity.checkType(kind, CompactMachines.PROJECTOR_BLOCK_ENTITY, ProjectorBlockEntity.tick)
}

class ProjectorBlockEntity(pos : BlockPos, state : BlockState, var isMainProjector : Boolean = false) extends BlockEntity(CompactMachines.PROJECTOR_BLOCK_ENTITY, pos, state) {
  // No need to serialize, field sets it
  var fieldRef : Option[IMiniaturizationField] = None
}
object ProjectorBlockEntity extends Ticker[ProjectorBlockEntity] {
  def tick(world: World, pos: BlockPos, state: BlockState, projector : ProjectorBlockEntity): Unit = {
    if (world.isClient) return
    if (projector.isMainProjector)
      projector.fieldRef.foreach(_.tick())
  }
}



