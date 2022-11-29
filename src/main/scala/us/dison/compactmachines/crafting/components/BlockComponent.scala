package us.dison.compactmachines.crafting.components

import net.minecraft.block.{Block, BlockState}
import net.minecraft.state.StateManager
import net.minecraft.util.registry.Registry
import us.dison.compactmachines.api.crafting.components.{IRecipeBlockComponent, RecipeComponentType}
import us.dison.compactmachines.*
import com.mojang.serialization.codecs.RecordCodecBuilder
import scala.collection.mutable
import net.minecraft.util.Identifier
import us.dison.compactmachines.util.codecs.ScalaMapCodec
import com.mojang.serialization.Codec
import us.dison.compactmachines.util.codecs.ScalaListCodec
import com.mojang.serialization.DataResult

class BlockComponent(private val block: Block, propertyRequirements: Map[String, List[String]] = null) extends IRecipeBlockComponent {
  private val validStates = mutable.Set[BlockState]()
  private var renderError = false
  private val filters = mutable.Map[String, Comparable[?] => Boolean]()
  private val allowedValues = mutable.Map[String, List[String]]()
  Option(propertyRequirements) match {
    case Some(p) =>
      validStates.clear()
      val stateManager = block.getStateManager
      p.foreach { case (k, v) =>
        validateAndAddUserFilter(stateManager, k, v)
      }
    case None => ()
  }
  buildValidStates()

  private def buildValidStates(): Unit = {
    val valid = block.getStateManager
      .getStates
      .stream()
      .filter(this.matches)
      .toScala(Set)
    validStates ++= valid
  }

  private def validateAndAddUserFilter(stateManager: StateManager[Block, BlockState], propertyName: String, value: List[String]): Unit = {
    val propmaybe = stateManager.getProperty(propertyName)
    Option(propmaybe).foreach { prop =>
      val userAllowed = mutable.ListBuffer[Any]()
      val propertyAcceptedValues = mutable.ListBuffer[String]()
      for (userValue <- value) {
        prop.parse(userValue).ifPresent { u =>
          propertyAcceptedValues += userValue
          userAllowed += u
        }
      }
      allowedValues.update(propertyName, propertyAcceptedValues.toList)
      filters.update(propertyName, userAllowed.contains)
    }
  }

  override def matches(state: BlockState): Boolean = {
    state.getBlock == block && {
      state.getProperties.stream().toScala(List)
        .forall { prop =>
          val name = prop.getName
          filters.get(name).forall(_ (state.get(prop)))
        }
    }
  }

  override def getBlock: Block = block

  override def getRenderState: BlockState = block.getDefaultState

  override def didErrorRendering(): Boolean = renderError

  override def markRenderingError(): Unit = renderError = true

  def hasFilter(property: String): Boolean =
    filters.contains(property)

  def getFirstMatch: Option[BlockState] =
    validStates.headOption

  override def toString: String =
    s"Block ${Registry.BLOCK.getKey(block)}"

  override def getType: RecipeComponentType[?] = CompactMachinesCraftingPlugin.BLOCK_COMPONENT

}

object BlockComponent {
  val blockIdCodec: Codec[Block] = Identifier.CODEC
    .flatXmap(id =>
      if (Registry.BLOCK.containsId(id))
        DataResult.success(Registry.BLOCK.get(id))
      else
        DataResult.error(s"Block $id is not registered")
      , (bl : Block) => DataResult.success(Registry.BLOCK.getKey(bl).get().getValue))
    .stable()
  val codec: Codec[BlockComponent] = RecordCodecBuilder.create(in => in.group(
    blockIdCodec.fieldOf("block").forGetter((it: BlockComponent) => it.getBlock()),
    ScalaMapCodec(Codec.STRING, ScalaListCodec(Codec.STRING)).optionalFieldOf("properties", Map()).forGetter((it: BlockComponent) => it.allowedValues.toMap)
  ).apply(in, (a: Block, b: Map[String, List[String]]) => BlockComponent(a, b))

  )
}