package us.dison.compactmachines.gui 

import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.font.MultilineText
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import us.dison.compactmachines.CompactMachines

@Environment(EnvType.CLIENT) 
object PSDScreen: 
  val TEXTURE: Identifier = Identifier(CompactMachines.MODID, "textures/gui/psd_screen.png")
class PSDScreen(title: Text) extends Screen(title): 
  override def render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) = 
    renderBackground(matrices)
    RenderSystem.setShader(() => GameRenderer.getPositionTexShader)
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    RenderSystem.setShaderTexture(0, PSDScreen.TEXTURE)
    val j : Int = (this.width - 256) / 2
    val k : Int = (this.height - 256) / 2 + 16 
    this.drawTexture(matrices, j, k, 0, 0, 256, 256);
    textRenderer.nn.drawWithShadow(matrices,
      Text.translatable(CompactMachines.MODID + ".psd.pages.machines.title"),
      j.toFloat+17, k.toFloat+16, Formatting.GOLD.getColorValue) 
    
    val text = MultilineText.create(textRenderer.nn, Text.translatable(CompactMachines.MODID + ".psd.pages.machines"), 222) 
    
    text.drawWithShadow(matrices, j + 17, k + 16 + 15, 10, Formatting.WHITE.getColorValue)
    
    super.render(matrices, mouseX, mouseY, delta) 
    


