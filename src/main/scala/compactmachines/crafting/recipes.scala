package us.dison.compactmachines.crafting.recipes 

import us.dison.compactmachines.crafting.catalyst.*
import us.dison.compactmachines.api.crafting.catalyst.*;
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
// import us.dison.compactmachines.crafting.recipes.layers.CCRecipeLayer
import us.dison.compactmachines.api.crafting.recipes.layers.CCRecipeBlocks
import us.dison.compactmachines.crafting.components.*
import us.dison.compactmachines.api.crafting.components.*;

import net.minecraft.util.math.Box
import us.dison.compactmachines.crafting.recipes.layers.dim.IDynamicSizedRecipeLayer
import net.minecraft.util.math.Vec3d
import us.dison.compactmachines.util.BlockSpaceUtil
import scala.collection.mutable
import net.minecraft.util.math.BlockPos
import com.mojang.serialization
import serialization.JsonOps
import serialization.Codec
import serialization.codecs.{RecordCodecBuilder, PrimitiveCodec}
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.DataResult
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.crafting.recipes.layers.dim.IFixedSizedRecipeLayer
import us.dison.compactmachines.crafting.recipes.blocks.ComponentPositionLookup
import us.dison.compactmachines.crafting.recipes.blocks.ComponentPositionLookupCodec
import us.dison.compactmachines.crafting.Registrar
import java.util.stream.Collectors
import scala.jdk.StreamConverters._
import net.minecraft.inventory.Inventory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.recipe.Recipe 
import net.minecraft.world.World
import net.minecraft.recipe.RecipeType
import net.minecraft.util.registry.Registry
import us.dison.compactmachines.api.crafting.recipes.layers.{CCRecipeLayer, RecipeLayerType, CCSymmetricLayer}
import net.minecraft.util.BlockRotation
import java.util.Arrays
import us.dison.compactmachines.api.crafting.field.FieldSize
import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Lifecycle
import java.util.Collections
import com.google.common.collect.ImmutableList
import net.minecraft.recipe.RecipeSerializer
import com.google.gson.JsonObject
import net.minecraft.network.PacketEncoderException
import io.netty.handler.codec.EncoderException
import net.minecraft.network.PacketByteBuf
import us.dison.compactmachines.crafting.components.IRecipeComponentsExt._
import us.dison.compactmachines.api.crafting.IMiniaturizationRecipe
import net.minecraft.world.WorldView
package blocks {
  class ComponentPositionLookup {
    protected[recipes] val components : mutable.Map[BlockPos, String] = mutable.Map[BlockPos, String]()
    protected[recipes] val componentTotals = mutable.Map[String, Int]()
    
    def add(location : BlockPos, component : String) : Unit = {
      components.updateWith(location)({ 
        case None => Some(component) 
        case Some(c) => Some(c)
      })
      componentTotals.updateWith(component)({
        case None => Some(1)
        case Some(c) => Some(c + 1)
      })

    }
    def getComponents() : scala.collection.Set[String] = {
      componentTotals.keySet
    }
    def getAllPositions() = components.keys.toList 
    def containsLocation(loc : BlockPos) = components.contains(loc)
    def getComponentTotals() = {
      componentTotals.toMap
    }
    protected[recipes] def rebuildComponentTotals() = {
      componentTotals.clear()
      val totals = components.values.groupMapReduce(identity)(_ => 1)(_ + _)
      componentTotals ++= totals
    }
    def getRequiredComponentKeyForPos(pos : BlockPos) = components.get(pos)
    def getPositionsForComponent(comp : String) : List[BlockPos] = {
      Option(comp) match {
        case None => List()
        case Some(c) => 
          components.iterator.filter(e => e._2 == c)
            .map(_._1)
            .toList
      }
    }
    def remove(comp : String) = {
      val positions = getPositionsForComponent(comp)
      positions.foreach(components -= _)
      componentTotals.remove(comp)
    }
  } 
  object ComponentPositionLookupCodec extends PrimitiveCodec[ComponentPositionLookup] {
    override def read[T](ops : DynamicOps[T], input : T) = {
      Codec.STRING.listOf().listOf().decode(ops, input).flatMap {s => 
        val layerList = s.getFirst()
        val lookup = ComponentPositionLookup()
  
        val zSize = layerList.size
        val mappedArray : Seq[Seq[String]] = 
          for (z <- 0 until zSize) yield {
            val layerComps = layerList.get(z)
            for (x <- 0 until layerComps.size) yield {
              layerComps.get(x)
            }
          }
        lookup.components ++= RecipeHelper.convertMultiListToMap(mappedArray.map(_.toList).toList)
        lookup.rebuildComponentTotals() 

        DataResult.success(lookup)

      }
    }
    override def write[T](ops : DynamicOps[T], lookup : ComponentPositionLookup) = {
      val boundsForBlocks = BlockSpaceUtil.getBoundsForBlocks(lookup.getAllPositions())
      val goodList = mutable.ListBuffer.fill[String](boundsForBlocks.getXLength.toInt, boundsForBlocks.getZLength.toInt)("-")
      BlockSpaceUtil.getBlocksIn(boundsForBlocks)
        .map(pos => (pos.toImmutable(), lookup.getRequiredComponentKeyForPos(pos).getOrElse("-")))
        .foreach { case (pos, comp) =>
          goodList(pos.getX()).update(pos.getZ(), comp)
        }
      val fin = goodList.map(_.asJava).asJava
      val encoded = Codec.STRING.listOf().listOf().encode(fin, ops, ops.empty())
      encoded.resultOrPartial(err => CompactMachines.LOGGER.error("Failed to encode layer component position lookup: {}", err)).get()
    }
  }
}
package catalyst {
  
}
package layers {

  import net.minecraft.block.BlockState
  import net.minecraft.util.math.Vec3i
  /*
  trait RecipeLayerType[L <: CCRecipeLayer] {
    def getCodec() : Codec[L]
  }
  trait CCRecipeLayer {
    def getComponents() : collection.Set[String]
    def getComponentTotals() : Map[String, Int]
    def getComponentForPosition(pos : BlockPos) : Option[String]
    def requiresAllBlocksIdentified() = true
    def matches(components : IRecipeComponents, blocks : CCRecipeBlocks) : Boolean = {
      !requiresAllBlocksIdentified() || blocks.allIdentified()
    }
    def getType() : RecipeLayerType[?]
    def dropNonRequiredComponents(components : IRecipeComponents) = ()
  }
  // marker trait
  trait CCSymmetricLayer {}
  
  trait CCRecipeBlocks {
    def getComponentAtPosition(relative : BlockPos) : Option[String]
    def getStateAtPosition(relative : BlockPos) : Option[BlockState]
    def getPositions() : List[BlockPos]
    def getNumberKnownComponents() : Int 
    def rebuildComponentTotals() : Unit 
    def getKnownComponentTotals() : Map[String, Int] 
    def getSourceBounds() : Box
    def allIdentified() : Boolean 
    def getUnmappedPositions() : List[BlockPos]
    def getPositionsForComponent(component : String) : List[BlockPos]
    def getFilledBounds() : Box 
    def slice(bounds : Box) : CCRecipeBlocks 
    def offset(amount : Vec3i) : CCRecipeBlocks 
    def below(offset : Int) = this.offset(Vec3i(0, -offset, 0))
    def above(offset : Int) = this.offset(Vec3i(0, offset, 0))
  }
  */
  extension (c : CCRecipeBlocks) {
    def componentAtPosition(relative : BlockPos) : Option[String] =
      c.getComponentAtPosition(relative).toScala
    def stateAtPosition(relative : BlockPos) : Option[BlockState] = 
      c.getStateAtPosition(relative).toScala
    def positions : List[BlockPos] = 
      c.getPositions().collect(Collectors.toList()).asScala.toList
    def numberKnownComponents : Int = 
      c.getNumberKnownComponents()
    def knownComponentTotals : Map[String, Int] = 
      c.getKnownComponentTotals().asScala.toMap.view.mapValues(_.toInt).toMap
    def sourceBounds : Box = 
      c.getSourceBounds() 
    def unmappedPositions : List[BlockPos] = 
      c.getUnmappedPositions().toScala(List)
    def positionsForComponent(component : String) : List[BlockPos] = 
      c.getPositionsForComponent(component).toScala(List)

    def filledBounds : Box = c.getFilledBounds()
  }
  class RecipeBlocks protected (box : Box)  extends CCRecipeBlocks {
    private var sourceBounds = box
    private var filledBounds = BlockSpaceUtil.getBoundsForBlocks(BlockSpaceUtil.getBlocksIn(sourceBounds))
    private val lookup = ComponentPositionLookup()
    private val states = mutable.Map[BlockPos, BlockState]() 
    private val unmatchedStates = mutable.Set[BlockPos]()
    private def copyInfoFrom(og : CCRecipeBlocks, offset : BlockPos) = {
      BlockSpaceUtil.getBlocksIn(sourceBounds).foreach { newPos => 
        val key = og.componentAtPosition(newPos)
        key match {
          case Some(k) => 
            lookup.add(newPos.add(offset), k)
          case None => 
            val unidentified = og.stateAtPosition(newPos)
            unidentified match {
              case None => () 
              case Some(s) => 
                if (!s.isAir()) 
                  unmatchedStates += newPos.add(offset).toImmutable()
            }

        }
        val oState = og.stateAtPosition(newPos).orNull
        states.update(newPos.add(offset).toImmutable(), oState)

      }
      rebuildComponentTotals()
    }
    override def getSourceBounds() = 
      sourceBounds 
    override def allIdentified() = 
      unmatchedStates.isEmpty 
    override def getUnmappedPositions() = 
      unmatchedStates.asJava.stream()
    override def getPositionsForComponent(comp : String) =
      lookup.getPositionsForComponent(comp).asJava.stream()
    override def getFilledBounds() = filledBounds 
    override def getComponentAtPosition(relative : BlockPos) = {
      lookup.components.get(relative).toJava
    } 
    override def getStateAtPosition(relative : BlockPos) = 
      states.get(relative).toJava 
    override def getPositions() =
      states.keySet.asJava.stream()
    override def getNumberKnownComponents() = 
      lookup.componentTotals.size 
    override def rebuildComponentTotals() = 
      lookup.rebuildComponentTotals()
    override def getKnownComponentTotals() = 
      lookup.componentTotals.map{
        case (k, v) => 
          (k, int2Integer(v))
      }.asJava
    override def slice(box : Box) = {
      val blocks = RecipeBlocks(sourceBounds.intersection(box))
      blocks.copyInfoFrom(this, BlockPos.ORIGIN)
      blocks
    }
    override def offset(amount : Vec3i) = {
      val copy = RecipeBlocks(filledBounds)
      copy.copyInfoFrom(this, BlockPos(amount))
      copy.filledBounds = copy.filledBounds.offset(BlockPos(amount))
      copy.sourceBounds = copy.sourceBounds.offset(BlockPos(amount))

      copy
    }
  }
  object RecipeBlocks {
    def copy(og : CCRecipeBlocks) = 
      val c = RecipeBlocks(og.getSourceBounds)
      c.copyInfoFrom(og, BlockPos.ORIGIN)
      c
    def empty = 
      RecipeBlocks(Box.of(Vec3d.ZERO, 0,0,0))
    def of(bounds : Box, states : Map[BlockPos, BlockState], components : Map[BlockPos, String], unmatchedStates : Set[BlockPos]) = {
      val blocks = RecipeBlocks(bounds)
      blocks.states ++= states 
      blocks.lookup.components ++= components 
      blocks.unmatchedStates ++= unmatchedStates
      blocks
    }
    def create(blocks : WorldView, components : IRecipeComponents, bounds : Box) = {
        val instance = RecipeBlocks(bounds)
        BlockSpaceUtil.getBlocksIn(bounds).map(_.toImmutable).foreach { pos => 

          val state = blocks.getBlockState(pos)

          instance.states.put(pos, state)

          val compKey = components.getKey(state).toScala
          compKey match {
            case Some(ck) => 
              instance.lookup.add(pos, ck)
            case None => 
              instance.unmatchedStates += pos
          }
        }
        instance
    }
    def rotate(og : CCRecipeBlocks, rotation : BlockRotation) : RecipeBlocks = {
      import us.dison.compactmachines.crafting.recipes.layers._
      if (rotation == BlockRotation.NONE) {
        RecipeBlocks.copy(og)
      } else {
        val originalPositions = og.getPositions().map(_.toImmutable).toScala(List)
        val rotated = BlockSpaceUtil.rotatePositions(originalPositions, rotation)
        val states = mutable.HashMap[BlockPos, BlockState]()
        val componentKeys = mutable.HashMap[BlockPos, String]()
        val unmatchedPositions = mutable.HashSet[BlockPos]()
        originalPositions.foreach { ogPos => 
          val rotatedPos = rotated(ogPos)
          states.put(rotatedPos, og.stateAtPosition(ogPos).orNull)
          og.componentAtPosition(ogPos).foreach(m => componentKeys.put(rotatedPos, m))

        }
        if (!og.allIdentified()) {
          og.unmappedPositions.foreach { pos => 
            val rotatedPos = rotated(pos).toImmutable()
            unmatchedPositions += rotatedPos
            og.componentAtPosition(pos).foreach(m => componentKeys(rotatedPos) = m)


          }
        }
        RecipeBlocks.of(og.sourceBounds, states.toMap, componentKeys.toMap, unmatchedPositions.toSet)
      }
    }
  }
  trait SCCRecipeLayer extends CCRecipeLayer {
    override def getComponentTotals() = 
      componentTotals.asJava.asInstanceOf
    def componentTotals : Map[String, Int]
    
    override def getComponentForPosition(pos : BlockPos) = 
      componentAtPosition(pos).toJava
    def componentAtPosition(pos : BlockPos) : Option[String]
    override def getPositionsForComponent(key : String) = 
      positionsForComponent(key).asJava.stream()
    def positionsForComponent(componentKey : String) : List[BlockPos] 
  }
  class FilledComponentRecipeLayer(val component : String = "-") extends SCCRecipeLayer with IDynamicSizedRecipeLayer with CCSymmetricLayer {
    // i frankly don't care about encapsulation 
    var recipeDimensions = Box.of(Vec3d.ZERO, 0, 0, 0)
    override def getComponents() = {
      Set(component).asJava
    }
    def getNumberFilledPositions() : Int = {
      Option(recipeDimensions) match {
        case Some(dim) => 
          Math.ceil(dim.getXLength() * dim.getZLength()).toInt
        case None => 
          0
      }
    }
    override def componentTotals = Map[String, Int]((component, getNumberFilledPositions()))
    override def componentAtPosition(pos: BlockPos) = {
      if (recipeDimensions.contains(pos.getX(), pos.getY(), pos.getZ()))
        Option(component)
      else 
        None
    }
    override def positionsForComponent(componentKey : String) = {
      if (component != componentKey) {
        List() 
      } else {
        BlockSpaceUtil.getBlocksIn(recipeDimensions) 
      }
    }
    override def matches(components: IRecipeComponents, blocks: CCRecipeBlocks): Boolean = {
      if (!blocks.allIdentified())
        false 
      else {
        val totalsInWorld = blocks.knownComponentTotals
        if (totalsInWorld.size != 1) 
          false 
        else {
          totalsInWorld.get(component).map(_ == getNumberFilledPositions()).getOrElse(false)
        }
      }
    }

    override def getType() = FilledComponentRecipeLayer
    override def setRecipeDimensions(box : Box) = this.recipeDimensions = box

  }
  object FilledComponentRecipeLayer extends RecipeLayerType[FilledComponentRecipeLayer] {
    val codec : Codec[FilledComponentRecipeLayer] = RecordCodecBuilder.create(in => in.group(Codec.STRING.fieldOf("component").forGetter((it : FilledComponentRecipeLayer) => it.component)).apply(in, it => FilledComponentRecipeLayer.apply(it)))
    def getCodec() = codec
  }
  class HollowComponentRecipeLayer(val component : String) extends SCCRecipeLayer with IDynamicSizedRecipeLayer with CCSymmetricLayer {
    private val filledPositions = mutable.Set.empty[BlockPos]
    private var recipeDimensions = Box.of(Vec3d.ZERO, 0, 0, 0)
    override def getComponents() = Set(component).asJava
    override def componentTotals = Map((component, getNumberFilledPositions()))
    override def componentAtPosition(pos: BlockPos): Option[String] = {
      if (filledPositions.contains(pos)) 
        Option(component)
      else 
        None
    }
    override def positionsForComponent(comp : String) : List[BlockPos] = 
      if (comp == component) 
        filledPositions.toList
      else 
        List()
    override def matches(components: IRecipeComponents, blocks: CCRecipeBlocks): Boolean = {
      if (!blocks.allIdentified()) {
        val anyNonAir = blocks.unmappedPositions.flatMap(blocks.stateAtPosition).exists(!_.isAir())
        if (anyNonAir) return false 
      }
      val totalsInWorld = blocks.knownComponentTotals
      // TODO: use match
      if (!totalsInWorld.contains(component)) {
        false 
      } else {
        if (totalsInWorld.size > 1) {
          val everythingElseEmpty = totalsInWorld.keySet.filter(c => c != component).forall(components.isEmptyBlock)
          if (!everythingElseEmpty) return false 
        }
        val targetCount = totalsInWorld.get(component).get
        val layerCount = filledPositions.size 

        layerCount == targetCount
      }
    } 
    override def setRecipeDimensions(dim : Box) = {
      recipeDimensions = dim
      }
    override def recalculateRequirements() = {
      this.filledPositions.clear()
      this.filledPositions.addAll(BlockSpaceUtil.getWallPositions(recipeDimensions).toSet)
    }
  
    override def getType() = HollowComponentRecipeLayer 
    def getNumberFilledPositions() = filledPositions.size 
  }
  object HollowComponentRecipeLayer extends RecipeLayerType[HollowComponentRecipeLayer] {
    override val getCodec : Codec[HollowComponentRecipeLayer] = RecordCodecBuilder.create(it => it.group(
        Codec.STRING.fieldOf("component").forGetter(_.component)
    ).apply(it, i =>HollowComponentRecipeLayer(i)))
  }

  class MixedComponentRecipeLayer(val componentLookup : ComponentPositionLookup = ComponentPositionLookup()) extends SCCRecipeLayer, IFixedSizedRecipeLayer {
    private val dimensions = BlockSpaceUtil.getBoundsForBlocks(componentLookup.getAllPositions())
    override def getDimensions() = dimensions
    override def getComponents() = componentLookup.getComponents().asJava
    override def dropNonRequiredComponents(components: IRecipeComponents): Unit = {
      components.getEmptyComponents().forEach(it => componentLookup.remove(it))
      val definedKeys = components.getBlockComponents().keySet 
      val toRemove = componentLookup.getComponents()
        .filter(layerComp => !definedKeys.contains(layerComp))
      toRemove.foreach(it => componentLookup.remove(it))
    }
    override def componentTotals: Map[String, Int] = 
      componentLookup.getComponentTotals()
    override def componentAtPosition(pos: BlockPos) : Option[String] = {
      componentLookup.getRequiredComponentKeyForPos(pos)
    }
    override def positionsForComponent(comp : String) = {
      componentLookup.getPositionsForComponent(comp)
    }
    def getNumberFilledPositions() = {
      componentTotals.values.reduce(_ + _)
    }
    override def matches(components: IRecipeComponents, blocks: CCRecipeBlocks): Boolean = {
      if (!blocks.allIdentified()) {
        val anyNonAir = blocks.unmappedPositions
          .flatMap(it => blocks.stateAtPosition(it))
          .exists(state => !state.isAir())
        if (anyNonAir) return false 
      }
      val requiredKeys = componentLookup.getComponents()
      val componentTotals = blocks.knownComponentTotals

      val missingComponents = requiredKeys.filter(rk => !componentTotals.contains(rk))
      if (!missingComponents.isEmpty) {
        false 
      } else {
        for (required <- requiredKeys) {
          val actual = blocks.getPositionsForComponent(required)
          val expected = componentLookup.getPositionsForComponent(required)
          if (expected != actual) return false
        }
        true 
      }
    }
    override def getType() = MixedComponentRecipeLayer
  }
  object MixedComponentRecipeLayer extends RecipeLayerType[MixedComponentRecipeLayer] {
    override val getCodec : Codec[MixedComponentRecipeLayer] = RecordCodecBuilder.create(in => in.group(
      ComponentPositionLookupCodec
        .fieldOf("pattern")
        .forGetter(_.componentLookup)
      ).apply(in, it => MixedComponentRecipeLayer(it)))
        
  }
  object RecipeLayerTypeCodec extends Codec[RecipeLayerType[?]] {
    def decode[T](ops : DynamicOps[T], input : T) = {
      Identifier.CODEC.decode(ops, input).flatMap {keyValuePair => 
        val id = keyValuePair.getFirst()
        val reg = Registrar.registry 
        if (!reg.contains(id)) {
        }
        reg.get(id) match {
          case None => 
            DataResult.error("Unknown registry key: " + id)
          case Some(i) => 
            DataResult.success(keyValuePair.mapFirst(_ => i))
        }
      }
    }
    def encode[T](input : RecipeLayerType[?], ops : DynamicOps[T], prefix : T) = {
      val reg = Registrar.registry
      val key = reg.find {
        case (k, v) => v == input
      }.map(_._1)
      key match {
        case None => DataResult.error("Unknown registry element " + input)
        case Some(k) => 
          val tomerge = ops.createString(key.toString())
          ops.mergeToPrimitive(prefix, tomerge)
      }
    }
  }
  package dim {
    trait IDynamicSizedRecipeLayer {
      def setRecipeDimensions(box : Box) : Unit 
      def setFieldSize(size : FieldSize) = {
        val dim = size.getDimensions
        val box = Box(0, 0, 0, dim, 1, dim)
        setRecipeDimensions(box)
      }
      def recalculateRequirements() = ()
    }
    trait IFixedSizedRecipeLayer {
      def getDimensions() : Box 
    }
  }
}
trait TMiniaturizationRecipe extends IMiniaturizationRecipe {
  override def getCatalyst() = catalyst 
  def catalyst : ICatalystMatcher 
  override def getOutputs() = outputs.toArray
  def outputs : List[ItemStack]
  override def getRecipeId() = identifier 
  def identifier : Identifier
  override def getCraftingTime() = craftingTime 
  def craftingTime : Int 
  override def getDimensions() = dimensions 
  def dimensions : Box 
  override def getLayer(layer : Int) = getLayerAt(layer).toJava 
  def getLayerAt(layer : Int) : Option[CCRecipeLayer]
  override def getComponents() = components 
  def components : IRecipeComponents
  override def setOutputs(outputs : java.util.Collection[ItemStack]) = 
    this.outputs = outputs.asScala.toList
  def outputs_=(outputs : List[ItemStack]) : Unit 
  override def getLayers() = 
    layers.asJavaSeqStream
  def layers : List[CCRecipeLayer]
}
trait EmptyInventory extends Inventory {
  override def size() = 0 
  override def isEmpty() = true 
  private def throwEmpty() : Nothing = 
    throw UnsupportedOperationException("This inventory is empty!")
  override def getStack(slot : Int) = throwEmpty()
  override def removeStack(slot : Int, amount : Int) = throwEmpty()
  override def removeStack(slot : Int) = throwEmpty() 
  override def setStack(slot : Int, stack : ItemStack) = throwEmpty() 
  override def markDirty() = ()
  override def canPlayerUse(player : PlayerEntity) = false 
  override def clear() = ()
}
trait CCRecipeBase extends Recipe[EmptyInventory] {
  override def matches(inv : EmptyInventory, worldIn : World) = true 
  override def craft(inv : EmptyInventory) = ItemStack.EMPTY
  override def fits(width : Int, height : Int) = true 
  override def getOutput() = ItemStack.EMPTY 
  override def isIgnoredInRecipeBook() = true 
  def setId(recipeId : Identifier) : Unit
}
import scala.beans.BeanProperty
class MiniturizationRecipe extends CCRecipeBase with TMiniaturizationRecipe {
  private var recipeSize = -1 
  @BeanProperty
  private var id : Identifier = null 
  private val layersMap = mutable.HashMap[Int, CCRecipeLayer]()
  var catalyst : ICatalystMatcher = null 
  override var outputs = List[ItemStack]()
  private var intldimensions = Box.of(Vec3d.ZERO, 0,0,0)
  private var cachedComponentTotals : Map[String, Int] = null
  override val components = MiniturizationRecipeComponents()
  override def identifier: Identifier = id 
  // override def setId(recipeId: Identifier): Unit = id = recipeId 
  override def dimensions = intldimensions
  override def getLayerAt(layer: Int) : Option[CCRecipeLayer] = 
    layersMap.get(layer)
  override def layers: List[CCRecipeLayer] = layersMap.values.toList
  override def craftingTime: Int = 200
  import us.dison.compactmachines.crafting.recipes.layers._
  def matchesBlocks(blocks : CCRecipeBlocks): Boolean = {
    BlockSpaceUtil.boundsFitsInside(blocks.filledBounds, dimensions) && {
      val filledBounds = blocks.filledBounds 
      val validRotations = BlockRotation.values()
      val layerRotationMatches = mutable.HashMap[BlockRotation, mutable.HashSet[Int]]()
      validRotations.foreach { rot => 
        layerRotationMatches(rot) = mutable.HashSet.fill(layersMap.size)(0)
      }
      for ((key, layer) <- layersMap.toMap) { 
        val layerblocks = blocks.slice(BlockSpaceUtil.getLayerBounds(blocks.filledBounds, key)).normalize()
        if (layer.requiresAllBlocksIdentified() && !layerblocks.allIdentified()) {
          return false 
        }
        val firstMatched = layer.matches(components, layerblocks)
        if (firstMatched) {
          layerRotationMatches(BlockRotation.NONE) += key
        }
        layer match {
          case _ : CCSymmetricLayer if (dimensions.getXLength() == dimensions.getZLength()) => 
            if (firstMatched) {
              for (rot <- Arrays.stream(BlockRotation.values()).filter(_ != BlockRotation.NONE).toScala(List)) {
                layerRotationMatches(rot) += key 
              }
            } 
            
          case _ => 
            Arrays.stream(BlockRotation.values()).toScala(List).filter(_ != BlockRotation.NONE).map { rot =>
              val rotated =  RecipeBlocks.rotate(layerblocks, rot)
              if (layer.matches(components, rotated)) {
                layerRotationMatches(rot) += key  
              }
            }
        }

        
      }
      layerRotationMatches.filter { case (key, value) => 
        value == layersMap.keySet 
      }.map(_._1).headOption.isDefined

    }
  }
  def applyComponents(compMap : Map[String, IRecipeComponent]) : Unit = {
    components.clear()
    for ((key, value) <- compMap) {
      value match {
        case v : IRecipeBlockComponent => 
          components.registerBlock(key,v) 
      }
    }
    for ((i, l) <- layersMap) {
      l.dropNonRequiredComponents(components)
      val missing = l.getComponents()
        .stream()
        .filter(key => !components.hasBlock(key))
        .toScala(Set)
      missing.foreach(needed => components.registerBlock(needed, EmptyBlockComponent()))
    }
  }
  def applyLayers(layers : List[CCRecipeLayer]) = {

    layersMap.clear()
    val rev = layers.reverse 
    for (y <- 0 until rev.size) {
      layersMap.put(y, rev(y))
    }
  }
  def getLayerListForCodecWrite() = {
    val arr = layersMap.keys.toArray
    scala.util.Sorting.quickSort[Int](arr)(using math.Ordering.Int.reverse)
    arr.toList.map(layersMap.apply)
  }
  def recalculateDimensions() = {
    val height = layersMap.size 
    var x = 0 
    var z = 0

    val hasAnyRigidLayers = layersMap.values.exists(l => l.isInstanceOf[us.dison.compactmachines.crafting.recipes.layers.dim.IFixedSizedRecipeLayer])
    if (!hasAnyRigidLayers) {
      this.intldimensions = Box(0,0,0, recipeSize, height, recipeSize) 
    } else {
      for (l <- layersMap.values) {
        l match {
          case layer : IFixedSizedRecipeLayer => 
            val dim = layer.getDimensions()
            if (dim.getXLength() > x) 
              x = Math.ceil(dim.getXLength()).toInt 
            if(dim.getZLength() > z) 
              z = Math.ceil(dim.getZLength()).toInt 
        }
      }
      this.intldimensions = Box(Vec3d.ZERO, Vec3d(x, height, z))
    }
    updateFluidLayerDimensions()
  }
  def updateFluidLayerDimensions() : Unit = {
    val footprint = BlockSpaceUtil.getLayerBounds(dimensions, 0)
    layersMap.values
      .filter(l => l.isInstanceOf[IDynamicSizedRecipeLayer])
      .foreach(dl => dl.asInstanceOf[IDynamicSizedRecipeLayer].setRecipeDimensions(footprint))
  }
  def fitsInFieldSize(fieldSize : FieldSize) = {
    val dim = fieldSize.getDimensions
    dimensions.getXLength() <= dim 
    && dimensions.getYLength() <= dim 
    && dimensions.getZLength() <= dim
  }

  def getComponentTotals() = {
    if (this.cachedComponentTotals != null) 
      this.cachedComponentTotals
    else {
      val totals = mutable.HashMap[String, Int]()
      components.allComponents.keySet.foreach { comp =>
        val count = getComponentRequiredCount(comp)
        totals.put(comp, count)
      }
      this.cachedComponentTotals = totals.toMap
      totals.toMap
    }
  }
  def getComponentRequiredCount(i : String) : Int = {
    if (!components.hasBlock(i))
      0 
    else {
      layersMap.values 
        .map(_.getComponentTotals)
        .map(totals => totals.get(i))
        .reduce(_ + _)
    }
  }
  def getNumberLayers() = {
    layersMap.size
  }
  def getRecipeSize() = recipeSize 
  def setRecipeSize(size : Int) = 
    if (FieldSize.canFitDimensions(size)) {
      this.recipeSize = size 
      recalculateDimensions() 
      true 
    } else false
  def hasSpecifiedSize() =
    FieldSize.canFitDimensions(this.recipeSize)
  // override def getRecipeIdentifier() = this.id
  override def getSerializer() = 
    MiniturizationRecipeSerializer
  override def getType() =
    CompactMachines.TYPE_MINITURIZATION_RECIPE
}
class CCBaseRecipeType[T <: CCRecipeBase](private val location : Identifier) extends RecipeType[T] {
  override def toString() = location.toString()
  def register() = {
    Registry.register(Registry.RECIPE_TYPE,location, this)
  }
}
object RecipeHelper {
  def convertSingleListToMap(list : List[String], x : Int) : Map[BlockPos, String] = {
    val map = mutable.Map[BlockPos, String]()
    for (z <- 0 until list.length) {
      val s = list(z)
      val relative = BlockPos(x, 0, z)
      map.put(relative, s)
    }
    map.toMap
  }
  def convertMultiListToMap(list : List[List[String]]) : Map[BlockPos, String] = {
    val map = mutable.Map[BlockPos, String]()
    for (x <- 0 until list.length) {
      val zValues = list(x)
      map ++= convertSingleListToMap(zValues, x)
    }
    map.toMap
  }
}

object MiniturizationRecipeCodec extends Codec[MiniturizationRecipe] {
  import layers.RecipeLayerTypeCodec
  val layerCodec : Codec[CCRecipeLayer] = RecipeLayerTypeCodec.dispatchStable(_.getType, _.getCodec)
  val componentCodec : Codec[IRecipeComponent] = 
    RecipeComponentTypeCodec.dispatchStable(_.getType(), _.getCodec())
  override def decode[T](ops : DynamicOps[T], input : T) : DataResult[Pair[MiniturizationRecipe, T]] = {
    val recipe = MiniturizationRecipe()
    val errorBuilder = StringBuilder()
    val recipeSize = Codec.INT.optionalFieldOf("recipeSize", -1)
      .codec()
      .parse(ops, input)
      .result()
      .get()
    val canFitLayers = recipe.setRecipeSize(recipeSize)
    val layers = layerCodec.listOf()
      .fieldOf("layers").codec()
      .parse(ops, input)
    if (layers.error().isPresent()) {
      val partialLayers = layers.resultOrPartial(errorBuilder.append)
      partialLayers.ifPresent(it => recipe.applyLayers(it.asScala.toList))
        CompactMachines.LOGGER.warn(errorBuilder.toString())
      DataResult.error(errorBuilder.toString(), Pair.of(recipe, input), Lifecycle.stable())
    } else {
      val layerList = layers.resultOrPartial(errorBuilder.append).orElse(Collections.emptyList()).asScala.toList
      recipe.applyLayers(layerList)
      val hasFixedLayers = layerList.exists(l => l.isInstanceOf[IFixedSizedRecipeLayer])
      if (!hasFixedLayers && !canFitLayers) {
        errorBuilder.append("Specified recipe size will not fit in a crafting field: ").append(recipeSize)
        CompactMachines.LOGGER.warn(errorBuilder.toString())
        DataResult.error(errorBuilder.toString(), Pair.of(recipe, input), Lifecycle.stable())
      } else {
        recipe.recalculateDimensions()
        val catalystNode = ops.get(input, "catalyst").result()
        if (catalystNode.isEmpty()) {
          CompactMachines.LOGGER.warn("No catalyst node defined in recipe; Likely bad file!")
        } else {
          val catalystType = ops.get(catalystNode.get(), "type").result()
          if (catalystType.isEmpty()) {
            CompactMachines.LOGGER.warn("Error : No catalyst type defined in recipe; falling back to itemstack")
            val stackData = ItemStack.CODEC 
              .fieldOf("catalyst").codec()
              .parse(ops, input)
              .resultOrPartial(errorBuilder.append)
              .orElse(ItemStack.EMPTY)
            recipe.catalyst = ItemStackCatalystMatcher.ofStack(stackData)
          } else {
            val catalyst = CatalystMatcherCodec.matcherCodec
              .fieldOf("catalyst").codec()
              .parse(ops, input)
              .resultOrPartial(errorBuilder.append)
              .orElse(ItemStackCatalystMatcher.ofStack(ItemStack.EMPTY))
            recipe.catalyst = catalyst
          }
        }
        val outputs = ItemStack.CODEC.listOf()
          .fieldOf("outputs")
          .codec()
          .parse(ops, input)
          .resultOrPartial(errorBuilder.append)
        outputs.ifPresent(it => recipe.outputs = it.asScala.toList)
        if (!outputs.isPresent()) {
          DataResult.error(errorBuilder.toString(), Pair.of(recipe, input), Lifecycle.stable())
        } else {
          if (recipe.outputs.length == 0) {  
            errorBuilder.append("No outputs were defined")
            CompactMachines.LOGGER.warn(errorBuilder.toString())
            DataResult.error(errorBuilder.toString(), Pair.of(recipe, input), Lifecycle.stable())
          } else {
            val components = Codec.unboundedMap(Codec.STRING, componentCodec)
              .optionalFieldOf("components", Collections.emptyMap())
              .codec()
              .parse(ops, input)
              .resultOrPartial(errorBuilder.append)
            components.ifPresent { compNode => 
              recipe.applyComponents(compNode.asScala.toMap)
            }
            DataResult.success(Pair.of(recipe, input), Lifecycle.stable())
          }
        }
      }
    }
  }
  override def encode[T](recipe : MiniturizationRecipe, ops : DynamicOps[T], prefix : T) : DataResult[T] = {
    if (recipe == null) {
      DataResult.error("Cannot serialize a null recipe")
    } else {
      val layers = layerCodec.listOf().encodeStart(ops, recipe.getLayerListForCodecWrite().asJava)
      val components = Codec.unboundedMap(Codec.STRING, componentCodec)
        .encodeStart(ops, recipe.components.getAllComponents())
      val catalystItem = recipe.catalyst 
      val catalyst :Option[DataResult[T]] = 
        if (catalystItem != null) {
          Some(CatalystMatcherCodec.matcherCodec.encodeStart(ops, catalystItem))
        } else {
          None
        }
      val outputs = ItemStack.CODEC.listOf()
        .encodeStart(ops, ImmutableList.copyOf(recipe.outputs.asJava))
      val builder = ops.mapBuilder()
      builder.add("type", Codec.STRING.encodeStart(ops, "compactmachines:miniturization_recipe"))
      if (recipe.hasSpecifiedSize()) {
        builder.add("recipeSize", Codec.INT.encodeStart(ops, recipe.getRecipeSize()))
      }
      val b = builder.add("layers", layers)
        .add("components", components)
      catalyst.foreach(it => b.add("catalyst", it))
      b.add("outputs", outputs).build(prefix)
    }
  }
}

object MiniturizationRecipeSerializer extends RecipeSerializer[MiniturizationRecipe] {
  override def read(recipeId : Identifier,  json : JsonObject) = {
    val parseResult = MiniturizationRecipeCodec.parse(JsonOps.INSTANCE, json)
    if (parseResult.error().isPresent()) {
      null 
    } else {
      parseResult.result()
        .map { r => 
          r.setId(recipeId)
          r 
        }.orElse(null)
    }
  }
  override def read(recipeId : Identifier, buffer : PacketByteBuf) : MiniturizationRecipe = {
    if (!buffer.isReadable() || buffer.readableBytes() == 0) {
      return null 
    } else {
      try {
        val recipe = buffer.decode(MiniturizationRecipeCodec)
        recipe.setId(recipeId)
        return recipe 
      } catch {
        case ex : EncoderException => null 
      } 
    }
  }
  override def write(buffer : PacketByteBuf, recipe : MiniturizationRecipe) = {
    buffer.encode(MiniturizationRecipeCodec, recipe)
  }
}
