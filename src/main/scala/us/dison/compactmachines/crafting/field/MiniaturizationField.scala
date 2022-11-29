package us.dison.compactmachines.crafting.field

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.minecraft.block.{Block, Blocks}
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Item
import net.minecraft.nbt.{NbtCompound, NbtElement, NbtOps}
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.structure.Structure
import net.minecraft.util.Identifier
import net.minecraft.util.math.{BlockBox, BlockPos, Box, ChunkPos}
import net.minecraft.util.registry.RegistryKey
import net.minecraft.world.{PersistentState, World, WorldAccess}
import net.minecraft.world.chunk.WorldChunk
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.api.crafting
import us.dison.compactmachines.api.crafting.{EnumCraftingState, IMiniaturizationRecipe}
import us.dison.compactmachines.api.crafting.catalyst.ICatalystMatcher
import us.dison.compactmachines.api.crafting.field.*
import us.dison.compactmachines.crafting.projector.FieldSizeExt.*
import us.dison.compactmachines.crafting.projector.{ProjectorBlock, ProjectorBlockEntity}
import us.dison.compactmachines.crafting.recipes.MiniaturizationRecipe
import us.dison.compactmachines.crafting.recipes.layers.RecipeBlocks
import us.dison.compactmachines.util.codecs.*
import us.dison.compactmachines.util.*

import java.util.Optional
import scala.beans.*
import scala.collection.mutable
import scala.collection.mutable.{HashMap, HashSet}
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.jdk.StreamConverters.*
import scala.math.Ordering
import scala.util.chaining.*


class MiniaturizationField(private var level : Option[World])  extends IMiniaturizationField {
  CompactMachines.LOGGER.info("Field initialized!")
  

  @BeanProperty
  private var fieldSize : FieldSize = FieldSize.INACTIVE

  @BeanProperty 
  private var center : BlockPos = _

  private var recipeId : Option[Identifier] = None

  def getRecipeId: Identifier = recipeId.orNull

  private var currentRecipe : Option[MiniaturizationRecipe] = None
  
  @BeanProperty
  var matchedBlocks : Structure = _

  @BeanProperty
  var craftingState : EnumCraftingState = EnumCraftingState.NOT_MATCHED


  private val listeners = mutable.HashSet[IFieldListener]()

  private var rescanTime : Long = 0
  @BeanProperty 
  var progress : Int = 0

  private var loaded : Boolean = true 

  var disabled_ = false 
  def disabled: Boolean = disabled_
  def disabled_=(bool : Boolean): Unit =
    if (bool) 
      disable()
    else enable()
  private var matchedCatalysts = Set[Item]()
  override def isLoaded: Boolean = loaded || level.map(_.isClient).getOrElse(false)

  override def cleanForDisposal(): Unit = {
    if (!level.get.isClient) {
      getProjectorPositions.forEach { pos =>
        val entity = level.get.getBlockEntity(pos)
        entity match {
          case e : ProjectorBlockEntity => 
            e.fieldRef = null 
          case _ => ()
        }
      }
    }
  }

  override def checkLoaded() : Unit = {
    
    // this.loaded = level.get.isRegionLoaded(center.getX(), center.getZ(), center.getX() + fieldSize.getProjectorDistance(), center.getZ() + fieldSize.getProjectorDistance())
    // sus
    this.loaded = true 
    if (loaded) {
      listeners.foreach(l => l.onFieldActivated(this))
    }
  }
  override def fieldContentsChanged() : Unit = {
    clearRecipe()

    rescanTime = level.map(_.getTime + 30).getOrElse(0)

  }
  override def setRecipe(id : Identifier) : Unit = {
    this.recipeId = Option(id)
    readRecipeFromId()
  }

  private def readRecipeFromId() : Unit = {
    for { 
      id <- recipeId 
      world <- level
    } {
        val r = world.getRecipeManager.get(id)
        if (!r.isPresent) {
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
  override def registerListener(listener : IFieldListener): Unit = listeners += listener
  override def getCurrentRecipe: Optional[MiniaturizationRecipe] = currentRecipe.toJava
  override def getBounds: BlockBox = this.fieldSize.getBoundsAtPosition(center)

  override def tick() : Unit = {
    if (level.isEmpty || disabled) 
      return
    val world = level.get
    if (rescanTime > 0 && world.getTime >= rescanTime) {
      doRecipeScan()
      rescanTime = 0 
      return 
    }  
    //if (getProjectorPositions.allMatch(world.isChunkLoaded))
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
        val catalystEntities = getCatalystsInField(world, Box.from(bounds).expand(0.25), currec.getCatalyst).asScala.toList
        if (catalystEntities.nonEmpty) {
          matchedCatalysts = catalystEntities.map(it => it.getStack.getItem).toSet

          if (!world.isClient) {
            CraftingHelper.consumeCatalystItem(catalystEntities.head, 1)
            clearBlocks() 
          }
          setCraftingState(EnumCraftingState.CRAFTING)
        }
      case EnumCraftingState.CRAFTING => 
        progress += 1 
        if (progress >= currec.getCraftingTime()) {
          for (is <- currec.getOutputs()) {
            val itemEntity = ItemEntity(world, center.getX + 0.5f, center.getY + 0.5f, center.getZ + 0.5f, is)
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
  def doRecipeScan() : Unit = {
    CompactMachines.LOGGER.info("starting scan")
    for (world <- level) {
      CompactMachines.LOGGER.info("Level exists!")
      val filledBlocks = getFilledBlocks

      if (filledBlocks.isEmpty) {
        clearRecipe()
        return 
      }

      val filledBounds = getFilledBounds

      val recipes = world.getRecipeManager
        .listAllOfType(CompactMachines.TYPE_MINIATURIZATION_RECIPE)
        .stream()
        .filter(recipe => BlockSpaceUtil.boundsFitsInside(recipe.getDimensions, filledBounds))
        .toScala(Set)
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
        CompactMachines.LOGGER.info(s"found recipe: ${recipe.getRecipeId.toString}")
        this.matchedBlocks = Structure() 
        val fieldBounds = fieldSize.getBoundsAtPosition(center)
        val minPos = BlockPos(fieldBounds.getMinX, fieldBounds.getMinY, fieldBounds.getMinZ)

        matchedBlocks.saveFromWorld(world, minPos, fieldSize.getBoundsAsBlockPos, false, null)

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
  def getFilledBlocks: Seq[BlockPos] =
    BlockSpaceUtil.getBlocksIn(getBounds()).filter(p => !level.get.isAir(p)).map(_.toImmutable)
  def getFilledBounds: BlockBox =
    BlockSpaceUtil.getBoundsForBlocks(getFilledBlocks)
  def clearRecipe(): Unit = {
    recipeId = None 
    currentRecipe = None 
    progress = 0 
    setCraftingState(EnumCraftingState.NOT_MATCHED)
    listeners.foreach { li => 
      li.onRecipeChanged(this, null)
      li.onRecipeCleared(this)
    }
  }
  def clearBlocks(): Unit = {
    val world = level.get
    getFilledBlocks
      .sortBy(_.getY)(using Ordering.Int.reverse)
      .foreach { blockPos => 
        world.setBlockState(blockPos, Blocks.AIR.getDefaultState, Block.NOTIFY_LISTENERS | Block.NOTIFY_NEIGHBORS | Block.NO_REDRAW)
        world match {
          case w : ServerWorld => 
            w.spawnParticles(ParticleTypes.LARGE_SMOKE,
              blockPos.getX + 0.5f, blockPos.getY + 0.5f, blockPos.getZ + 0.5f,
              1, 0d, 0.05d, 0d, 0.25d)
          case _ => ()
        }
      }
  }
  override def checkRedstone(): Unit = {
    this.disabled = getProjectorPositions.anyMatch(proj => level.get.getReceivedRedstonePower(proj) > 0)

    if (disabled) disable() else enable()
  } 
  override def enable(): Unit = {
    this.disabled_ = false
    fieldContentsChanged()
    getProjectorPositions.forEach { proj => 
      ProjectorBlock.activateProjector(level.get, proj, fieldSize)

    }
  }

  override def setWorld(world : World): Unit = level = Some(world)

  override def isEnabled: Boolean = !disabled

  override def disable(): Unit = {
    disabled_ = true
    if (craftingState != EnumCraftingState.NOT_MATCHED) 
      handleDestabilize()
    getProjectorPositions.forEach { proj =>
      ProjectorBlock.deactivateProjector(level.get, proj)
    }
    // TODO: Networking
  }
  override def serverData(): NbtCompound = {
     val nbt = NbtCompound()
     nbt.putString("size", fieldSize.name())
     nbt.putLong("center", getCenter().asLong())
     nbt.putString("state", craftingState.name())

     currentRecipe.foreach { it => 
      nbt.putString("recipe", it.getRecipeId.toString)
      nbt.putInt("progress", progress)
     }
    
     if (matchedBlocks != null) 
       nbt.put("matchedBlocks", matchedBlocks.writeNbt(NbtCompound()))

     nbt.putBoolean("disabled", disabled)

     nbt


  }

  ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) =>
    if (level.exists(_.getRegistryKey == world.getRegistryKey)) {
      this.checkLoaded()
    }
  )
  ServerChunkEvents.CHUNK_LOAD.register((world, chunk) =>
    if (level.contains(world)) {
      this.checkLoaded()
    }
  )
}

object MiniaturizationField {
  val codec : Codec[IMiniaturizationField] = RecordCodecBuilder.create(in => in.group(
      FieldSize.CODEC.fieldOf("size").forGetter((it : IMiniaturizationField) => it.getFieldSize),
      BlockPos.CODEC.fieldOf("center").forGetter((it : IMiniaturizationField) => it.getCenter),
      EnumCraftingState.CODEC.fieldOf("state").forGetter((it : IMiniaturizationField) => it.getCraftingState),
      Identifier.CODEC.fieldOf("recipe").forGetter((it : IMiniaturizationField) => it.getRecipeId),
      ScalaIntCodec.fieldOf("progress").forGetter((it : IMiniaturizationField) => it.getProgress),
      NbtCompound.CODEC.fieldOf("matchedBlocks").forGetter((it : IMiniaturizationField) => Option(it.getMatchedBlocks).map((i : Structure) => i.writeNbt(NbtCompound())).orNull),
      ScalaBooleanCodec.fieldOf("enabled").forGetter((it : IMiniaturizationField) => it.isEnabled)
    ).apply(in, (size : FieldSize, center : BlockPos, state : EnumCraftingState, recipe : Identifier,  progress : Int, matchedBlocksNbt  : NbtCompound, enabled : Boolean) => {
      val matchedBlocks : Structure = Structure().tap(_.readNbt(matchedBlocksNbt))
      val field = MiniaturizationField(None)
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
  def ofSizeAndCenter(fieldSize : FieldSize, center : BlockPos): MiniaturizationField = {
    val field = MiniaturizationField(None)
    field.center = center 
    field.fieldSize = fieldSize 
    field.craftingState = EnumCraftingState.NOT_MATCHED 
    field
  }

}
