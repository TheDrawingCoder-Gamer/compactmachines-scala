package us.dison.compactmachines.crafting.recipes.layers.dim

import net.minecraft.util.math.{BlockBox, Box}
import us.dison.compactmachines.api.crafting.field.FieldSize

trait IDynamicSizedRecipeLayer {
  def dimensions : BlockBox
  def dimensions_=(box: BlockBox): Unit

  def setFieldSize(size: FieldSize): Unit = {
    val dim = size.getDimensions
    val box = BlockBox(0, 0, 0, dim, 1, dim)
    this.dimensions = box
  }

  def recalculateRequirements(): Unit = ()
}
