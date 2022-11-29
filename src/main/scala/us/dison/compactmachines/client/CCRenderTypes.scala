package us.dison.compactmachines.client 

import net.minecraft.client.render.RenderLayer 
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.RenderPhase
object CCRenderTypes {
  val FieldRenderType : RenderLayer = RenderLayer.of("projection_field", 
    VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, 256,
      RenderLayer.MultiPhaseParameters.builder()
      .shader(RenderPhase.COLOR_SHADER)
      .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
      .cull(RenderPhase.Cull(true))
      .writeMaskState(RenderPhase.WriteMaskState(true, false))
      .build(false)
    )
  val PhantomRenderType : RenderLayer = RenderLayer.of("phantom", 
    VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 2097152, true, false,
    RenderLayer.MultiPhaseParameters.builder()
    .shader(RenderPhase.BLOCK_SHADER)
    .lightmap(RenderPhase.ENABLE_LIGHTMAP)
    .texture(RenderPhase.MIPMAP_BLOCK_ATLAS_TEXTURE)
    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
    .build(true)
  )
}
