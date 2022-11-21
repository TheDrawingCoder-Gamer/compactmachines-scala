package us.dison.compactmachines.crafting.field 

import us.dison.compactmachines.api.crafting.field.*;
import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import us.dison.compactmachines.api.crafting 
import us.dison.compactmachines.crafting.projector.{ProjectBlock, ProjectorBlockEntity}
import crafting.catalyst.ICatalystMatcher
import crafting.IMiniaturizationRecipe
import crafting.EnumCraftingState 
import scala.collection.mutable.{HashSet, HashMap}
import scala.jdk.OptionConverters._
import scala.jdk.StreamConverters._
import scala.jdk.CollectionConverters._
import net.minecraft.world.World
import us.dison.compactmachines.crafting.projector.FieldSizeExt._
import net.minecraft.structure.Structure
import net.minecraft.world.PersistentState
import net.minecraft.world.WorldAccess
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Item
import us.dison.compactmachines.util.{CraftingHelper, BlockSpaceUtil}
import net.minecraft.util.Identifier 
import us.dison.compactmachines.crafting.recipes.MiniturizationRecipe
import us.dison.compactmachines.CompactMachines
import scala.math.Ordering
import us.dison.compactmachines.crafting.recipes.layers.RecipeBlocks
import net.minecraft.nbt.NbtCompound
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import scala.util.chaining.* 
import net.minecraft.util.math.ChunkPos
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.NbtElement
import net.minecraft.server.world.ServerWorld
import us.dison.compactmachines.util._
import us.dison.compactmachines.util.codecs.*
/*
trait TMiniaturizationField extends IMiniaturizationField {
  override def getBounds() = bounds
  def bounds : Box
  
  override def getFieldSize() = fieldSize 
  def fieldSize : FieldSize

  override def setFieldSize(a : FieldSize) = fieldSize = a 
  def fieldSize_=(size : FieldSize) : Unit

  override def getCenter() = center 
  def center : BlockPos

  override def setCenter(a : BlockPos) = center = a 
  def center_=(center : BlockPos) : Unit 

  
}
*/
class ActiveWorldFields(var level : World = null) extends PersistentState {
  private val fields = HashMap[BlockPos, IMiniaturizationField]()
  def setLevel(level : World) = this.level = level 
  def getFields() = fields.values.toList
  def tickFields() : Unit = {
    val loaded = fields.values.filter(_.isLoaded()).toSet 

    if (loaded.isEmpty) 
      return 

    loaded.foreach(_.tick)
  }
  def addFieldInstance(field : IMiniaturizationField) = {
    field.setWorld(level)

    val center = field.getCenter()

    fields.put(center, field)
  }
  def registerField(field : IMiniaturizationField) : IMiniaturizationField = {
    val anyMissing = ProjectBlock.getMissingProjectorsSize(level, field.getFieldSize(), field.getCenter()).nonEmpty 

    if (anyMissing)
      return field 

    addFieldInstance(field)
    field.getProjectorPositions().forEach { pos => 
      val stateAt = level.getBlockState(pos)
      // We would have had a missing and returned early if any weren't a projector block 
      assume(stateAt.getBlock().isInstanceOf[ProjectBlock], "Should have returned early")
      if (stateAt.hasBlockEntity()) {
        val tileAt = level.getBlockEntity(pos)
        tileAt match {
          case e : ProjectorBlockEntity => 
            e.fieldRef = Option(field)
          case _ => 
            CompactMachines.LOGGER.error("Entity at projector block isn't a projector block entity!")
        }
      }
    }

    field 
  }

  def unregisterField(center : BlockPos) : Unit = {
    if (fields.contains(center)) {
      val removed = fields.remove(center).get 
      // TODO: networking 
      removed.cleanForDisposal()
    }
  }
  def unregisterField(field : IMiniaturizationField) : Unit = 
    unregisterField(field.getCenter())
  def get(center : BlockPos) : Option[IMiniaturizationField] = fields.get(center)
  def hasActiveField(center : BlockPos) = fields.contains(center)
  def getFields(chunk : ChunkPos) = 
    fields.filter { case (key, _) => 
      ChunkPos(key) == chunk 
    }.map(_._2)

  def levelId = level.getRegistryKey()

  override def writeNbt(tag : NbtCompound ) = {
    ActiveWorldFields.codec.encodeStart(NbtOps.INSTANCE, fields.values.toList.asJava)
      .result()
      .ifPresent(goodTag => tag.put("cm_fields", goodTag))
    tag
  }

}
object ActiveWorldFields {
  val key = "compactmachines_fields"
  val codec = MiniaturizationField.codec.listOf()

  def fromTag(tag : NbtCompound) = {
    val worldFields = ActiveWorldFields()
    codec.parse(NbtOps.INSTANCE, tag.getList("cm_fields", NbtElement.COMPOUND_TYPE))
      .result()
      .ifPresent(f =>  worldFields.fields ++= Map.from(f.asScala.map(v => (v.asInstanceOf[IMiniaturizationField].getCenter(), v.asInstanceOf[IMiniaturizationField]))))
    worldFields
  }
  def get(world : World) = {
    val server = world.asInstanceOf[ServerWorld]
    val fields = server.getPersistentStateManager.getOrCreate[ActiveWorldFields](ActiveWorldFields.fromTag, () => ActiveWorldFields(world), key)
    fields.setLevel(world)
    fields
  }
}
import scala.beans.*;
import net.minecraft.block.Blocks
import net.minecraft.block.Block
import net.minecraft.server.world.ServerWorld
import net.minecraft.particle.ParticleTypes
class MiniaturizationField  extends IMiniaturizationField {
  CompactMachines.LOGGER.info("Field initialized!")
  @BeanProperty
  private var fieldSize : FieldSize = FieldSize.INACTIVE

  @BeanProperty 
  private var center : BlockPos = null 

  private var recipeId : Option[Identifier] = None

  def getRecipeId() = recipeId.orNull 

  private var currentRecipe : Option[MiniturizationRecipe] = None
  
  @BeanProperty
  var matchedBlocks : Structure = null

  @BeanProperty
  var craftingState : EnumCraftingState = EnumCraftingState.NOT_MATCHED

  private var level : Option[World] = None

  private val listeners = HashSet[IFieldListener]()

  private var rescanTime : Long = 0
  @BeanProperty 
  var progress : Int = 0

  private var loaded : Boolean = false 

  var disabled_ = false 
  def disabled = disabled_ 
  def disabled_=(bool : Boolean) = 
    if (bool) 
      disable()
    else enable()
  private var matchedCatalysts = Set[Item]()
  override def isLoaded() = loaded || level.map(_.isClient).getOrElse(false)

  override def cleanForDisposal() = {
    if (!level.get.isClient) {
      getProjectorPositions().forEach { pos => 
        val entity = level.get.getBlockEntity(pos)
        entity match {
          case e : ProjectorBlockEntity => 
            e.fieldRef = null 
          case _ => ()
        }
      }
    }
  }

  override def checkLoaded() = {
    CompactMachines.LOGGER.debug("Checking loaded state.")
    this.loaded = level.get.isRegionLoaded(center.getX(), center.getZ(), center.getX() + fieldSize.getProjectorDistance() + 3, center.getZ() + fieldSize.getProjectorDistance + 3)
    if (loaded) {
      listeners.foreach(l => l.onFieldActivated(this))
    }
  }
  override def fieldContentsChanged() = {
    clearRecipe()

    rescanTime = level.get.getTime() + 30 

  }
  override def setRecipe(id : Identifier) = {
    this.recipeId = Option(id)
    getRecipeFromId()
  }

  private def getRecipeFromId() : Unit = {
    for { 
      id <- recipeId 
      world <- level
    } {
        val r = world.getRecipeManager().get(id)
        if (!r.isPresent()) {
          clearRecipe()
           return 
        }
        r.ifPresent { rec => 
          this.currentRecipe = Some(rec.asInstanceOf)
          if (craftingState == EnumCraftingState.NOT_MATCHED) {
            setCraftingState(EnumCraftingState.MATCHED)
          }
          this.listeners.foreach { li => 
            li.onRecipeChanged(this, this.currentRecipe.get)
            li.onRecipeMatched(this, this.currentRecipe.get)
          }
        }
    }
    if (recipeId.isEmpty || level.isEmpty) 
      clearRecipe()
  }
  override def registerListener(listener : IFieldListener) = listeners += listener
  override def getCurrentRecipe() = currentRecipe.toJava
  override def getBounds() = this.fieldSize.getBoundsAtPosition(center)

  override def tick() : Unit = {
    if (level.isEmpty || disabled) 
      return
    val world = level.get
    if (rescanTime > 0 && world.getTime >= rescanTime) {
      doRecipeScan()
      rescanTime = 0 
      return 
    }  
    if (getProjectorPositions().allMatch(world.isChunkLoaded)) 
      tickCrafting() 
  }
  private def tickCrafting() : Unit = {
    val bounds = getBounds()

    if (level.isEmpty || this.currentRecipe.isEmpty)
      return 
    val currec = currentRecipe.get 
    val world = this.level.get
    craftingState match {
      case EnumCraftingState.MATCHED => 
        val catalystEntities = getCatalystsInField(world, bounds.expand(0.25), currec.getCatalyst()).asScala.toList
        if (catalystEntities.size > 0) {
          matchedCatalysts = catalystEntities.map(it => it.getStack().getItem()).toSet

          if (!world.isClient) {
            CraftingHelper.consumeCatalystItem(catalystEntities(0), 1)
            clearBlocks() 
          }
          setCraftingState(EnumCraftingState.CRAFTING)
        }
      case EnumCraftingState.CRAFTING => 
        progress += 1 
        if (progress >= currec.getCraftingTime()) {
          for (is <- currec.getOutputs()) {
            val itemEntity = ItemEntity(world, center.getX() + 0.5f, center.getY() + 0.5f, center.getZ() + 0.5f, is)
            world.spawnEntity(itemEntity)
          }
          val completed = currec 

          clearRecipe()

          listeners.foreach( l => l.onRecipeCompleted(this, completed))
        }
      case _ => ()
    }
  }
  private def getCatalystsInField(level : WorldAccess, fieldBounds : Box, itemFilter : ICatalystMatcher) = {
    level.getEntitiesByClass(classOf[ItemEntity], fieldBounds, it => itemFilter.matches(it.getStack))
  }
  // currently causes scala to freak out
  def doRecipeScan() : Unit = {
    CompactMachines.LOGGER.info("starting scan")
    for (world <- level) {
      CompactMachines.LOGGER.info("Level exists!")
      val filledBlocks = getFilledBlocks()

      if (filledBlocks.length == 0) {
        clearRecipe()
        return 
      }

      val filledBounds = getFilledBounds()

      val recipes = world.getRecipeManager()
        .listAllOfType(CompactMachines.TYPE_MINITURIZATION_RECIPE)
        .stream()
        .filter(recipe => BlockSpaceUtil.boundsFitsInside(recipe.asInstanceOf[MiniturizationRecipe].getDimensions(), filledBounds))
        .toScala(Set)
        .asInstanceOf[Set[MiniturizationRecipe]]
      if (recipes.isEmpty) {
        clearRecipe()
        return 
      }

      currentRecipe = None 
      recipeId = None 
      progress = 0 
      val goodRecipe = recipes.find { recipe => 
        val blocks = RecipeBlocks.create(world, recipe.getComponents(), filledBounds)
        recipe.matchesBlocks(blocks)
      }
      goodRecipe.foreach {recipe => 
        CompactMachines.LOGGER.info("found recipe: " + recipe.getRecipeId.toString())
        this.matchedBlocks = Structure() 
        val fieldBounds = fieldSize.getBoundsAtPosition(center)
        val minPos = BlockPos(fieldBounds.minX, fieldBounds.minY, fieldBounds.minZ)

        matchedBlocks.saveFromWorld(world, minPos, fieldSize.getBoundsAsBlockPos(), false, null)

        currentRecipe = Some(recipe)
        recipeId = Some(recipe.getRecipeId())
      }
      setCraftingState(currentRecipe.map(_ => EnumCraftingState.MATCHED).getOrElse(EnumCraftingState.NOT_MATCHED))
      val currec = currentRecipe.orNull 
      // TODO: Networking 
      listeners.foreach {li => 
        li.onRecipeChanged(this, currec)
        if (craftingState == EnumCraftingState.MATCHED) 
          li.onRecipeMatched(this, currentRecipe.get)
      }
    }
  }
  def getFilledBlocks() = 
    BlockSpaceUtil.getBlocksIn(getBounds()).filter(p => !level.get.isAir(p)).map(_.toImmutable)
  def getFilledBounds() = 
    BlockSpaceUtil.getBoundsForBlocks(getFilledBlocks())
  def clearRecipe() = {
    recipeId = None 
    currentRecipe = None 
    progress = 0 
    setCraftingState(EnumCraftingState.NOT_MATCHED)
    listeners.foreach { li => 
      li.onRecipeChanged(this, null)
      li.onRecipeCleared(this)
    }
  }
  def clearBlocks() = {
    val world = level.get
    getFilledBlocks()
      .sortBy(_.getY)(using Ordering.Int.reverse)
      .foreach { blockPos => 
        world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.NOTIFY_NEIGHBORS | Block.NO_REDRAW)
        world match {
          case w : ServerWorld => 
            w.spawnParticles(ParticleTypes.LARGE_SMOKE,
              blockPos.getX() + 0.5f, blockPos.getY() + 0.5f, blockPos.getZ() + 0.5f,
              1, 0d, 0.05d, 0d, 0.25d)
          case _ => ()
        }
      }
  }
  override def checkRedstone() = {
    this.disabled = getProjectorPositions().anyMatch(proj => level.get.getReceivedRedstonePower(proj) > 0)

    if (disabled) disable() else enable()
  } 
  override def enable() = {
    this.disabled = false
    fieldContentsChanged()
    getProjectorPositions.forEach { proj => 
      ProjectBlock.activateProjector(level.get, proj, fieldSize)

    }
  }

  override def setWorld(world : World) = level = Some(world)

  override def enabled() = !disabled 

  override def disable() = {
    disabled = true 
    if (craftingState != EnumCraftingState.NOT_MATCHED) 
      handleDestabilize()
    getProjectorPositions().forEach { proj => 
      ProjectBlock.deactivateProjector(level.get, proj)
    }
    // TODO: Networking
  }
  override def serverData() = {
     val nbt = NbtCompound()
     nbt.putString("size", fieldSize.name())
     nbt.putLong("center", getCenter().asLong())
     nbt.putString("state", craftingState.name())

     currentRecipe.foreach { it => 
      nbt.putString("recipe", it.getRecipeId().toString())
      nbt.putInt("progress", progress)
     }
    
     if (matchedBlocks != null) 
       nbt.put("matchedBlocks", matchedBlocks.writeNbt(NbtCompound()))

     nbt.putBoolean("disabled", disabled)

     nbt


  }

}

object MiniaturizationField {
  val codec : Codec[IMiniaturizationField] = RecordCodecBuilder.create(in => in.group(
      FieldSize.CODEC.fieldOf("size").forGetter((it : IMiniaturizationField) => it.getFieldSize()),
      BlockPos.CODEC.fieldOf("center").forGetter((it : IMiniaturizationField) => it.getCenter()),
      EnumCraftingState.CODEC.fieldOf("state").forGetter((it : IMiniaturizationField) => it.getCraftingState()),
      Identifier.CODEC.fieldOf("recipe").forGetter((it : IMiniaturizationField) => it.getRecipeId()),
      ScalaIntCodec.fieldOf("progress").forGetter((it : IMiniaturizationField) => it.getProgress()),
      NbtCompound.CODEC.fieldOf("matchedBlocks").forGetter((it : IMiniaturizationField) => Option(it.getMatchedBlocks()).map((i : Structure) => i.writeNbt(NbtCompound())).orNull),
      ScalaBooleanCodec.fieldOf("enabled").forGetter((it : IMiniaturizationField) => it.enabled())
    ).apply(in, (size : FieldSize, center : BlockPos, state : EnumCraftingState, recipe : Identifier,  progress : Int, matchedBlocksNbt  : NbtCompound, enabled : Boolean) => {
      val matchedBlocks : Structure = Structure().tap(_.readNbt(matchedBlocksNbt))
      val field = MiniaturizationField()
      field.setRecipe(recipe)
      field.setCenter(center)
      field.setCraftingState(state) 
      field.setProgress(progress)
      Option(matchedBlocksNbt).foreach { it =>
        val matchedBlocks = Structure().tap(_.readNbt(it))
        field.matchedBlocks = matchedBlocks
      } 
      field.disabled = !enabled
      field.fieldSize = size
      field
    }))

  def ofSizeAndCenter(fieldSize : FieldSize, center : BlockPos) = { 
    val field = MiniaturizationField()
    field.center = center 
    field.fieldSize = fieldSize 
    field.craftingState = EnumCraftingState.NOT_MATCHED 
    field
  }
}
