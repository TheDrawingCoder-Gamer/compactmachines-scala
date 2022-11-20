package us.dison.compactmachines.crafting.components

import com.mojang.serialization.Codec
import net.minecraft.block.BlockState
import net.minecraft.block.Block
import scala.collection.mutable
import net.minecraft.state.StateManager
import java.util.stream.Collectors
import scala.jdk.StreamConverters._
import scala.jdk.CollectionConverters._
import net.minecraft.util.registry.Registry
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.datafixers.util.Pair
import net.minecraft.util.Identifier
import com.mojang.serialization.codecs.RecordCodecBuilder
import us.dison.compactmachines.util.{ScalaListCodec, ScalaMapCodec}
import us.dison.compactmachines.CompactMachines
import net.minecraft.block.Blocks
trait IRecipeComponent {
  // why ?
  def getType() : RecipeComponentType[?]
}
trait IRecipeBlockComponent extends IRecipeComponent {
  def matches(state : BlockState) : Boolean 
  def getBlock() : Block
  def getRenderState() : BlockState
  def didErrorRendering() : Boolean 
  def markRenderingError() : Unit 
}
class BlockComponent(private val block : Block, propertyRequirements : Map[String, List[String]] = null ) extends IRecipeBlockComponent {
  private val validStates = mutable.Set[BlockState]()
  private var renderError = false 
  private val filters = mutable.Map[String, Comparable[?] => Boolean]()
  private val allowedValues = mutable.Map[String, List[String]]()
  Option(propertyRequirements) match {
    case Some(p) => 
      validStates.clear()
      val stateManager = block.getStateManager()
      p.foreach { case (k, v) => 
        validateAndAddUserFilter(stateManager, k, v)
      }
    case None => ()
  }
  buildValidStates()
  private def buildValidStates() : Unit = {
    val valid = block.getStateManager()
      .getStates()
      .stream()
      .filter(this.matches)
      .toScala(Set)
    validStates ++= valid
  }
  private def validateAndAddUserFilter(stateManager : StateManager[Block, BlockState], propertyName : String, value : List[String]) = {
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
    state.getBlock() == block && {
      state.getProperties().stream().toScala(List)
      .forall { prop => 
        val name = prop.getName()
        filters.get(name).forall(_(state.get(prop)))
      }
    }
  }
  override def getBlock(): Block = block 
  override def getRenderState(): BlockState = block.getDefaultState()
  override def didErrorRendering(): Boolean = renderError 
  override def markRenderingError(): Unit = renderError = true 
  def hasFilter(property : String) = 
    filters.contains(property)
  def getFirstMatch() =
    validStates.headOption 
  override def toString(): String = 
    s"Block ${Registry.BLOCK.getKey(block)}"
  override def getType(): RecipeComponentType[?] = ComponentRegistry.BLOCK_COMPONENT

}
object BlockComponent {
  val blockIdCodec : Codec[Block] = Identifier.CODEC 
    .flatXmap(id => 
        if (Registry.BLOCK.containsId(id)) 
          DataResult.success(Registry.BLOCK.get(id))
        else 
          DataResult.error(s"Block $id is not registered")
      , bl => DataResult.success(Registry.BLOCK.getKey(bl).get().getValue()))
    .stable()
  val codec : Codec[BlockComponent] = RecordCodecBuilder.create(in => in.group(
    blockIdCodec.fieldOf("block").forGetter((it : BlockComponent) => it.getBlock()),
    ScalaMapCodec(Codec.STRING, ScalaListCodec(Codec.STRING)).fieldOf("properties").forGetter((it : BlockComponent) => it.allowedValues.toMap)
      ).apply(in, (a : Block, b : Map[String, List[String]]) => BlockComponent(a, b))

  ) 
}
class EmptyBlockComponent extends IRecipeComponent, IRecipeBlockComponent {
  override def matches(state : BlockState) = state.isAir()
  override def getBlock() = Blocks.AIR 
  override def getRenderState(): BlockState = Blocks.AIR.getDefaultState()
  override def didErrorRendering(): Boolean = false 
  override def markRenderingError(): Unit = ()
  override def getType() = ComponentRegistry.EMPTY_BLOCK_COMPONENT
}
object EmptyBlockComponent {
  val codec = Codec.unit(() => EmptyBlockComponent())
}
trait RecipeComponentType[C <: IRecipeComponent] {
  def getCodec() : Codec[C]
}
class SimpleRecipeComponentType[C <: IRecipeComponent](private val s : Codec[C]) extends RecipeComponentType[C] {
  override def getCodec() = s
}
object ComponentRegistry {
  protected[compactmachines] val COMPONENTS = mutable.HashMap[Identifier, RecipeComponentType[?]]()
  val BLOCK_COMPONENT = SimpleRecipeComponentType(BlockComponent.codec)
  val EMPTY_BLOCK_COMPONENT = SimpleRecipeComponentType(EmptyBlockComponent.codec)
  COMPONENTS.put(Identifier(CompactMachines.MODID, "block"), BLOCK_COMPONENT)
  COMPONENTS.put(Identifier(CompactMachines.MODID, "empty"), EMPTY_BLOCK_COMPONENT)

}
object RecipeComponentTypeCodec extends Codec[RecipeComponentType[?]] {
  override def decode[T](ops: DynamicOps[T], input: T): DataResult[Pair[RecipeComponentType[?], T]] = {
    Identifier.CODEC.decode(ops, input).flatMap { rl => 
      val resource = rl.getFirst()
      ComponentRegistry.COMPONENTS.get(resource) match {
        case Some(value) => 
          DataResult.success(rl.mapFirst(_ => value))
        case None => 
          DataResult.error("Unknown registry key " + resource)
      }
    } 
  }
  override def encode[T](input: RecipeComponentType[?], ops: DynamicOps[T], prefix: T): DataResult[T] = {
    val key = ComponentRegistry.COMPONENTS.view.find {
      case (k, v) => v == input 
    }.map(_._1)
    key match {
      case Some(key) => 
        val tomerge = ops.createString(key.toString())
        ops.mergeToPrimitive(prefix, tomerge)
      case None => 
        DataResult.error("Unknown registry element " + input) 
    }
  }
}
trait IRecipeComponents {
  def getAllComponents() : Map[String, IRecipeComponent]
  def getBlockComponents() : Map[String, IRecipeBlockComponent]
  def isEmptyBlock(key : String) : Boolean 
  def getBlock(key : String) : Option[IRecipeBlockComponent]
  def hasBlock(key : String) : Boolean 
  def registerBlock(key : String, component : IRecipeBlockComponent) : Unit 
  def unregisterBlock(key : String) : Unit 
  def registerOther(key : String, component : IRecipeComponent) : Unit 
  def size() : Int 
  def clear() : Unit 
  def getKey(state : BlockState) : Option[String]
  def getEmptyComponents() : List[String]
}

class MiniturizationRecipeComponents extends IRecipeComponents {
  private val blockComponents = mutable.HashMap[String, IRecipeBlockComponent]()
  private val otherComponents = mutable.HashMap[String, IRecipeComponent]()
  override def getAllComponents(): Map[String, IRecipeComponent] = {
    blockComponents.toMap ++ otherComponents.toMap
  }
  override def getBlockComponents(): Map[String, IRecipeBlockComponent] = 
    blockComponents.toMap 
  override def isEmptyBlock(key: String): Boolean = {
    blockComponents.get(key) match {
      case None => true 
      case Some(comp) => 
        comp.isInstanceOf[EmptyBlockComponent]
    }
  }
  override def hasBlock(key: String): Boolean = blockComponents.contains(key)
  override def registerBlock(key: String, component: IRecipeBlockComponent): Unit = 
    blockComponents.put(key, component)
  override def unregisterBlock(key: String): Unit = 
    blockComponents.remove(key)
  override def clear(): Unit = {
    blockComponents.clear()
    otherComponents.clear()
  }
  override def getBlock(key: String): Option[IRecipeBlockComponent] = {
    blockComponents.get(key)
  }
  override def getEmptyComponents(): List[String] = {
    blockComponents.toMap.filter {
      case (k, v) => v.isInstanceOf[EmptyBlockComponent]
    }.keys.toList
  }
  override def getKey(state: BlockState): Option[String] = {
    blockComponents.find{
      case (k, v) => 
        v.matches(state)
    }.map(_._1)
  }
  override def registerOther(key: String, component: IRecipeComponent): Unit = {
    otherComponents(key) = component 
  }
  override def size(): Int = 
    otherComponents.size + blockComponents.size
}
