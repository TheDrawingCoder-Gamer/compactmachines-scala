package us.dison.compactmachines.crafting.components

import net.minecraft.block.{BlockState, Blocks}
import us.dison.compactmachines.api.crafting.components.{IRecipeBlockComponent, IRecipeComponent, RecipeComponentType}
import us.dison.compactmachines.*
import com.mojang.serialization.Codec

class EmptyBlockComponent extends IRecipeComponent, IRecipeBlockComponent {
  override def matches(state: BlockState): Boolean = state.isAir

  override def getBlock: Block = Blocks.AIR

  override def getRenderState: BlockState = Blocks.AIR.getDefaultState

  override def didErrorRendering(): Boolean = false

  override def markRenderingError(): Unit = ()

  override def getType: RecipeComponentType[_] = CompactMachinesCraftingPlugin.EMPTY_BLOCK_COMPONENT
}

object EmptyBlockComponent {
  val codec: Codec[EmptyBlockComponent] = Codec.unit(EmptyBlockComponent())
}