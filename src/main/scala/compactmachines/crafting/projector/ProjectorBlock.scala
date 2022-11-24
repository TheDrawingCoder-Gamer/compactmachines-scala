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
import scala.jdk.CollectionConverters._
import scala.jdk.StreamConverters._
import us.dison.compactmachines.util._
import us.dison.compactmachines.util.Ticker 
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
import us.dison.compactmachines.api.crafting.field.FieldSize; 
import FieldSizeExt._
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
object FieldSizeExt {
  extension (fs : FieldSize) {
    def fieldSize = fs.name()
    def projectorPoses = fs.getProjectorPoses().toScala(List)
    def projectorPosesAt(pos : BlockPos) = fs.getProjectorPosesAt(pos).toScala(List)
    def originCenter = fs.getOriginCenter()
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

object ProjectBlock {
  val FACING = DirectionProperty.of("facing", Direction.Type.HORIZONTAL)
  val FIELD_SIZE : EnumProperty[FieldSize] = EnumProperty.of("field_size", classOf[FieldSize])
  val BASE = VoxelShapes.cuboid(0, 0, 0, 1, 6 / 16d, 1)
  val POLE = VoxelShapes.cuboid(7 / 16d, 6 / 16d, 7 / 16d, 9 / 16d, 14 / 16d, 9 / 16d)
  val SHAPE = VoxelShapes.union(BASE, POLE)
  val SPOOKY = BooleanProperty.of("spooky") 
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
    // exists == .map(???).getOrElse(false)
    getDirection(world, pos).exists(projFacing => size.getCenterFromProjector(pos, projFacing) == actualCenter)
  }
  def getOpposite(world : BlockView, proj : BlockPos, size : FieldSize) = {
    getDirection(world, proj).map(it => size.getOppositeProjector(proj, it))
  }
  def hasProjectorOpposite(world : BlockView, initial : BlockPos, size : FieldSize, initialFacing : Direction) = {
    val oppositePos = size.getOppositeProjector(initial, initialFacing)
    val oppositeState = world.getBlockState(oppositePos)
    oppositeState.getBlock() match {
      case _ : ProjectBlock => 
        val oppFacing = oppositeState.get(FACING)
        oppFacing.getDirection() == initialFacing
      case _ => 
        false
    } 
  }
  def hasValidCross(world : BlockView, initial : BlockPos, size : FieldSize, initialFacing : Direction) = {
    val center = size.getCenterFromProjector(initial, initialFacing)
    val crossAxis = initialFacing.getAxis().crossAxis
    val crossAxiesPos = size.projectorPosesAxis(center, crossAxis)
    crossAxiesPos.exists { pos  => 
      projectorFacesCenter(world, pos, center, size)
    }
  }
  def getSmallestOpposite(world : BlockView, initial : BlockPos, facing : Direction) = {
    FieldSize.VALID_SIZES.find( it => hasProjectorOpposite(world, initial, it, facing))
  }
  def getSmallestCross(world : BlockView, initial : BlockPos, facing : Direction) = {
    FieldSize.VALID_SIZES.find( it => hasValidCross(world, initial, it, facing))
  }

  def getFieldCenter(state : BlockState, projector : BlockPos) = 
    state.get(FIELD_SIZE).getCenterFromProjector(projector, state.get(FACING))
  def getSmallest(world : BlockView, initial : BlockPos, facing : Direction) = {
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
  def getMissingProjectorsSize(world : BlockView, size : FieldSize, center : BlockPos) = {
    size.projectorPosesAt(center).filterNot(it => projectorFacesCenter(world, it, center, size))
  }
  def getMissingProjectors(world : BlockView, proj : BlockPos, dir : Direction) = {
    getSmallest(world, proj, dir).map { size => 
      val center = size.getCenterFromProjector(proj, dir)
      getMissingProjectorsSize(world, size, center) 
    }.getOrElse(getValidOppositePositions(proj, dir))
    
  }
  def getValidOppositePositions(pos : BlockPos, facing : Direction) = {
    FieldSize.VALID_SIZES.map(it => it.getOppositeProjector(pos, facing)).toList
  }
  def isActive(state : BlockState) = {
    state.get[FieldSize](FIELD_SIZE) != FieldSize.INACTIVE
  }
  def activateProjector(level : World, pos : BlockPos, fieldSize : FieldSize) = {
    if (level.isChunkLoaded(pos)) {
      val state =level.getBlockState(pos)
      state.getBlock() match {
        case _ : ProjectBlock => 
          if (state.get(FIELD_SIZE) != fieldSize) {
            val newState = state.copy(FIELD_SIZE, fieldSize)
            level.setBlockState(pos, newState, Block.NOTIFY_ALL)
          }
        case _ => ()
      }

    }
  }
  def deactivateProjector(level : World, pos : BlockPos) = {
    val curState = level.getBlockState(pos) 
    curState.getBlock() match {
      case _ : ProjectBlock => 
        val newState = curState.copy(FIELD_SIZE, FieldSize.INACTIVE)
        level.setBlockState(pos, newState, Block.NOTIFY_ALL)
      case _ => ()
    }
  }
}

class ProjectBlock(settings : AbstractBlock.Settings) extends BlockWithEntity(settings) {
  setDefaultState(stateManager.getDefaultState()
    .`with`(ProjectBlock.FACING, Direction.NORTH)
    .`with`[FieldSize, FieldSize](ProjectBlock.FIELD_SIZE, FieldSize.INACTIVE)
    // .`with`(ProjectBlock.SPOOKY, false)
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

  override def getPlacementState(ctx: ItemPlacementContext): BlockState = {
   val looking = 
     Option(ctx.getPlayer()).map( it => if it.isSneaking() then ctx.getPlayerFacing().getOpposite() else ctx.getPlayerFacing()).getOrElse(ctx.getPlayerFacing())

   getDefaultState().copy(ProjectBlock.FACING, looking)
  }
  @annotation.nowarn("cat=deprecation")
  override def onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult = {
    if (world.isClient()) {
      val hasMissing = !ProjectBlock.getMissingProjectors(world, pos, state.get(ProjectBlock.FACING)).isEmpty
      if (hasMissing) {
        HologramRenderer.resetRenderTime()
        HologramRenderer.setProjector(world, pos)
      }
    }
    ActionResult.SUCCESS
  }

  override def onBlockAdded(state: BlockState, world: net.minecraft.world.World, pos: BlockPos, oldState: BlockState, b : Boolean) = { 
    val missing = ProjectBlock.getMissingProjectors(world, pos, state.get(ProjectBlock.FACING))
    val hasMissing = missing.exists(_ != pos)
    val size : FieldSize = 
      if (hasMissing) {
        CompactMachines.LOGGER.info("Missing projectors: " + missing)
        FieldSize.INACTIVE 
      } else {
        ProjectBlock.getSmallest(world, pos, state.get(ProjectBlock.FACING)).getOrElse(FieldSize.INACTIVE).tap(it => CompactMachines.LOGGER.info(s"No missing, field size $it"))
      }
    val fieldcenter = size.getCenterFromProjector(pos, state.get(ProjectBlock.FACING))

    
    if (size == FieldSize.INACTIVE) 
      return 
    if (!hasMissing) {
      world.setBlockState(pos, state.copy(ProjectBlock.FIELD_SIZE, size), Block.NOTIFY_ALL)
      val server = world.getServer()
      if (server == null) 
        return 
      if (world.isRegionLoaded(fieldcenter.getX(), fieldcenter.getZ(), fieldcenter.getX() + size.getProjectorDistance(), fieldcenter.getZ() + size.getProjectorDistance())) {
        size.getProjectorPosesAt(fieldcenter).forEach( proj => ProjectBlock.activateProjector(world, proj, size))


        val center = ProjectBlock.getFieldCenter(state, pos)

        val fields = ActiveWorldFields.getOrCreate(world)
        if (!fields.hasActiveField(center)) {
          val field = fields.registerField(MiniaturizationField.ofSizeAndCenter(size,center))
          field.fieldContentsChanged()

          // TODO: Networking 
        }

      }
    }
  }
  override def onStateReplaced(state: BlockState, world: net.minecraft.world.World, pos: BlockPos, newState: BlockState, moved: Boolean): Unit = {
    val center = ProjectBlock.getFieldCenter(state, pos)
    val size = state.get(ProjectBlock.FIELD_SIZE)

    if (ProjectBlock.isActive(state)) {
      size.getProjectorPosesAt(center).forEach(proj => ProjectBlock.deactivateProjector(world, proj))

      if (!world.isClient()) {
        val serverWorld = world.asInstanceOf[ServerWorld] 
        val fields = ActiveWorldFields.getOrCreate(serverWorld)
        if (fields.hasActiveField(center)) {
          val field = fields.get(center)
          field match {
            case Some(fl) => 
              if (fl.enabled()) {
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
    val facing = state.get(ProjectBlock.FACING)
    // North projector is the ticker 
    ProjectorBlockEntity(pos, state, facing == Direction.SOUTH)
  }

  override def neighborUpdate(state: BlockState, world: net.minecraft.world.World, pos: BlockPos, block: Block, fromPos: BlockPos, notify: Boolean): Unit = {
    super.neighborUpdate(state, world, pos, block, fromPos, notify)

    if (world.isClient) 
      return 

    if (ProjectBlock.isActive(state)) {
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
      ProjectBlock.getSmallestOpposite(world, pos, state.get(ProjectBlock.FACING)).foreach { size => 
        val center : BlockPos = size.getCenterFromProjector(pos, state.get(ProjectBlock.FACING))
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
    if (world.isClient) 
      return 
    if (projector.isMainProjector)
      projector.fieldRef.foreach(_.tick())
  } 
}
object HologramRenderer { 
    private var renderTime = 0
    private val missing = mutable.HashMap[BlockPos, Direction]()
    private val baseState = CompactMachines.BLOCK_PROJECTOR.getDefaultState()
    def tick() = {
      if (renderTime > 0) renderTime -= 1 
      if (renderTime < 0) renderTime = 0
    }
    def resetRenderTime() = 
      renderTime = 200
    def render(stack : MatrixStack) = {
      if (renderTime != 0 && !missing.isEmpty) {
        val mc = MinecraftClient.getInstance()
        val buffers = mc.getBufferBuilders().getEntityVertexConsumers()
        val camera = mc.gameRenderer.getCamera()
        val level = mc.world 

        renderMissing(stack, buffers, level)
      }
    }
    
    protected[projector] def renderMissing(stack : MatrixStack, 
                                        buffers : VertexConsumerProvider.Immediate, world : ClientWorld) : Unit = {
      stack.push()
      val camera = MinecraftClient.getInstance().gameRenderer.getCamera()
      val projectedView = camera.getPos()
      stack.translate(-projectedView.x, -projectedView.y, -projectedView.z)
      for ((pos, dir) <- missing) {
        stack.push()
        stack.translate(
          pos.getX() + 0.05d,
          pos.getY() + 0.05d,
          pos.getZ() + 0.05d
         )
        stack.scale(.9f, .9f, .9f)
        SolidColorRenderer.renderTransparentBlock(baseState.copy(ProjectBlock.FACING, dir), pos, stack, buffers, renderTime)
        stack.pop()
        var y = -1 
        var isAir = true 
        while (y > -10 && isAir) {
          val realPos = BlockPos(pos.getX(), pos.getY() + y, pos.getZ())
          isAir = world.testBlockState(realPos, _.isAir())
          if (isAir) {
            stack.push()
            stack.translate(
              pos.getX() + 0.15d,
              pos.getY() + 0.15d + y, 
              pos.getZ() + 0.15d)
            stack.scale(0.6f, 0.6f, 0.6f)
            SolidColorRenderer.renderTransparentBlock(Blocks.BLACK_STAINED_GLASS.getDefaultState(), realPos, stack, buffers, renderTime)
            stack.pop()
          }
          y -= 1
        }
      }
      // RenderSystem.disableDepthTest()
      buffers.draw(CCRenderTypes.PhantomRenderType)
      // RenderSystem.enableDepthTest()
      stack.pop()
    }
    
   /*
    protected def renderMissing(stack : MatrixStack, buffers : VertexConsumerProvider.Immediate, world : ClientWorld) : Unit = {
      stack.push()
      val renderer = MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer()
      val manager = MinecraftClient.getInstance().getBakedModelManager()
      val consumer = buffers.getBuffer(CCRenderTypes.PhantomRenderType)
      for ((pos, dir) <- missing) {
        val state = baseState.copy(ProjectBlock.FACING, dir)
        val model = manager.getBlockModels().getModel(state)  
        renderer.render(world, model, state, pos, stack, consumer, true, world.getRandom(), LightmapTextureManager.MAX_SKY_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV)
      }
      buffers.draw(CCRenderTypes.PhantomRenderType)
      stack.pop()
    } */
    def setProjector(world : World, initial : BlockPos) = {
      missing.clear()
      val initialFacing = ProjectBlock.getDirection(world, initial).getOrElse(Direction.UP)
      val fieldSize = ProjectBlock.getSmallest(world, initial, initialFacing)
      fieldSize match {
        case Some(size) =>
          val center = size.getCenterFromProjector(initial, initialFacing)
          
          Direction.Type.HORIZONTAL.forEach { dir => 
            if (dir.getOpposite() != initialFacing) {
              val location = size.getProjLocationWithDirection(center, dir)
              if (!world.getBlockState(location).getBlock().isInstanceOf[ProjectBlock])
                missing.put(location, dir.getOpposite())
            }

          }
        case None => 
          ProjectBlock.getValidOppositePositions(initial, initialFacing).foreach { pos => 
            missing.put(pos, initialFacing.getOpposite())
          }
      }
    } 

}

object SolidColorRenderer {
  def renderTransparentBlock(state : BlockState, pos : BlockPos | Null, stack : MatrixStack, buffer : VertexConsumerProvider, ticksLeft : Int = 100) : Unit = {
    val mc = MinecraftClient.getInstance()
    val colors = mc.getBlockColors()
    
    val alpha = 
      if (ticksLeft >= 100) {
        0.9f 
      } else {
        0.9f * Math.max(ticksLeft / 100f, 0.1f)
      }
    val builder = buffer.getBuffer(CCRenderTypes.PhantomRenderType)
    val dispatcher = mc.getBlockRenderManager()
    val model = dispatcher.getModel(state)
    if (model != mc.getBakedModelManager().getMissingModel()) {
      for (dir <- Direction.values()) {
        model.getQuads(state, dir, mc.world.random).forEach(it => addQuad(state, pos, stack, mc, colors, builder, it, alpha))
      }
      model.getQuads(state, null, mc.world.random).forEach(it => addQuad(state, pos, stack, mc, colors, builder, it, alpha))
    }

  }
  private def addQuad(state : BlockState, pos : BlockPos | Null, stack : MatrixStack, 
                      mc : MinecraftClient, colors : BlockColors, builder : VertexConsumer, 
                      quad : BakedQuad, alpha : Float) : Unit = {
    val color = 
      if (quad.hasColor()) {
        colors.getColor(state, mc.world, pos, quad.getColorIndex())
      } else {
        ColorHelper.Argb.getArgb(255, 255,255,255)
      }
    val red = ColorHelper.Argb.getRed(color) / 255f 
    val green = ColorHelper.Argb.getGreen(color) / 255f
    val blue = ColorHelper.Argb.getBlue(color) / 255f
    val trueAlpha = MathHelper.clamp(0.01f, alpha, 0.06f)
    builder.quad(stack.peek(), quad, red, green, blue, LightmapTextureManager.MAX_SKY_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV)
  }
} 
