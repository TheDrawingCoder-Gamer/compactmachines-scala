package us.dison.compactmachines.crafting.recipes

import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import io.netty.handler.codec.EncoderException
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.util.Identifier

object MiniaturizationRecipeSerializer extends RecipeSerializer[MiniaturizationRecipe] {
  override def read(recipeId: Identifier, json: JsonObject): MiniaturizationRecipe = {
    val parseResult = MiniaturizationRecipeCodec.parse(JsonOps.INSTANCE, json)
    if (parseResult.error().isPresent) {
      null
    } else {
      parseResult.result()
        .map { r =>
          r.setId(recipeId)
          r
        }.orElse(null)
    }
  }

  override def read(recipeId: Identifier, buffer: PacketByteBuf): MiniaturizationRecipe = {
    if (!buffer.isReadable() || buffer.readableBytes() == 0) {
      null
    } else {
      try {
        val recipe = buffer.decode(MiniaturizationRecipeCodec)
        recipe.setId(recipeId)
        recipe
      } catch {
        case ex: EncoderException => null
      }
    }
  }

  override def write(buffer: PacketByteBuf, recipe: MiniaturizationRecipe): Unit = {
    buffer.encode(MiniaturizationRecipeCodec, recipe)
  }
}
