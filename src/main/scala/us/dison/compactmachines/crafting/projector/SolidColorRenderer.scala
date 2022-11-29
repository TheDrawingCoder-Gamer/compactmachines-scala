package us.dison.compactmachines.crafting.projector

import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.color.block.BlockColors
import net.minecraft.client.render.model.BakedQuad
import net.minecraft.client.render.{LightmapTextureManager, OverlayTexture, VertexConsumer, VertexConsumerProvider}
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.{BlockPos, ColorHelper, Direction, MathHelper}
import us.dison.compactmachines.client.CCRenderTypes

object SolidColorRenderer {
  def renderTransparentBlock(state: BlockState, pos: BlockPos | Null, stack: MatrixStack, buffer: VertexConsumerProvider, ticksLeft: Int = 100): Unit = {
    val mc = MinecraftClient.getInstance()
    val colors = mc.getBlockColors

    val alpha =
      if (ticksLeft >= 100) {
        0.9f
      } else {
        0.9f * Math.max(ticksLeft / 100f, 0.1f)
      }
    val builder = buffer.getBuffer(CCRenderTypes.PhantomRenderType)
    val dispatcher = mc.getBlockRenderManager
    val model = dispatcher.getModel(state)
    if (model != mc.getBakedModelManager.getMissingModel) {
      for (dir <- Direction.values()) {
        model.getQuads(state, dir, mc.world.random).forEach(it => addQuad(state, pos, stack, mc, colors, builder, it, alpha))
      }
      model.getQuads(state, null, mc.world.random).forEach(it => addQuad(state, pos, stack, mc, colors, builder, it, alpha))
    }

  }

  private def addQuad(state: BlockState, pos: BlockPos | Null, stack: MatrixStack,
                      mc: MinecraftClient, colors: BlockColors, builder: VertexConsumer,
                      quad: BakedQuad, alpha: Float): Unit = {
    val color =
      if (quad.hasColor) {
        colors.getColor(state, mc.world, pos, quad.getColorIndex)
      } else {
        ColorHelper.Argb.getArgb(255, 255, 255, 255)
      }
    val red = ColorHelper.Argb.getRed(color) / 255f
    val green = ColorHelper.Argb.getGreen(color) / 255f
    val blue = ColorHelper.Argb.getBlue(color) / 255f
    val trueAlpha = MathHelper.clamp(0.01f, alpha, 0.06f)
    builder.quad(stack.peek(), quad, red, green, blue, LightmapTextureManager.MAX_SKY_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV)
  }
}
