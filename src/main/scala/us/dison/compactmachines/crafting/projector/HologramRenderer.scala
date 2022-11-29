package us.dison.compactmachines.crafting.projector

import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.world.ClientWorld
import net.minecraft.util.math.{BlockPos, Direction}
import net.minecraft.world.World
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.client.CCRenderTypes

import scala.collection.mutable

object HologramRenderer {
  private var renderTime = 0
  private val missing : mutable.Map[BlockPos, Direction] = mutable.HashMap()
  private val baseState = CompactMachines.BLOCK_PROJECTOR.getDefaultState

  def tick(): Unit = {
    if (renderTime > 0) renderTime -= 1
    if (renderTime < 0) renderTime = 0
  }

  def resetRenderTime(): Unit =
    renderTime = 200

  def render(stack: MatrixStack): Unit = {
    if (renderTime != 0 && missing.nonEmpty) {
      val mc = MinecraftClient.getInstance()
      val buffers = mc.getBufferBuilders.getEntityVertexConsumers
      val camera = mc.gameRenderer.getCamera
      val level = mc.world

      renderMissing(stack, buffers, level)
    }
  }

  protected[projector] def renderMissing(stack: MatrixStack,
                                         buffers: VertexConsumerProvider.Immediate, world: ClientWorld): Unit = {
    stack.push()
    val camera = MinecraftClient.getInstance().gameRenderer.getCamera
    val projectedView = camera.getPos
    stack.translate(-projectedView.x, -projectedView.y, -projectedView.z)
    for ((pos, dir) <- missing) {
      stack.push()
      stack.translate(
        pos.getX + 0.05d,
        pos.getY + 0.05d,
        pos.getZ + 0.05d
      )
      stack.scale(.9f, .9f, .9f)
      SolidColorRenderer.renderTransparentBlock(baseState.`with`(ProjectorBlock.FACING, dir), pos, stack, buffers, renderTime)
      stack.pop()
      var y = -1
      var isAir = true
      while (y > -10 && isAir) {
        val realPos = BlockPos(pos.getX, pos.getY + y, pos.getZ)
        isAir = world.testBlockState(realPos, _.isAir())
        if (isAir) {
          stack.push()
          stack.translate(
            pos.getX + 0.15d,
            pos.getY + 0.15d + y,
            pos.getZ + 0.15d)
          stack.scale(0.6f, 0.6f, 0.6f)
          SolidColorRenderer.renderTransparentBlock(Blocks.BLACK_STAINED_GLASS.getDefaultState, realPos, stack, buffers, renderTime)
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
  def setProjector(world: World, initial: BlockPos): Unit = {
    missing.clear()
    val initialFacing = ProjectorBlock.getDirection(world, initial).getOrElse(Direction.UP)
    val fieldSize = ProjectorBlock.getSmallest(world, initial, initialFacing)
    fieldSize match {
      case Some(size) =>
        val center = size.getCenterFromProjector(initial, initialFacing)

        Direction.Type.HORIZONTAL.forEach { dir =>
          if (dir.getOpposite != initialFacing) {
            val location = size.getProjLocationWithDirection(center, dir)
            if (!world.getBlockState(location).getBlock.isInstanceOf[ProjectorBlock])
              missing.put(location, dir.getOpposite)
          }

        }
      case None =>
        ProjectorBlock.getValidOppositePositions(initial, initialFacing).foreach { pos =>
          missing.put(pos, initialFacing.getOpposite)
        }
    }
  }

}
