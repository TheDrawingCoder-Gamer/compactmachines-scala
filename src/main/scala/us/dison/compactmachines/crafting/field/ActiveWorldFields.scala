package us.dison.compactmachines.crafting.field

import com.mojang.serialization.Codec
import net.minecraft.nbt.{NbtCompound, NbtElement, NbtOps}
import net.minecraft.util.math.{BlockPos, ChunkPos}
import net.minecraft.world.{PersistentState, World}
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.api.crafting.field.IMiniaturizationField
import us.dison.compactmachines.crafting.projector.{ProjectorBlock, ProjectorBlockEntity}
import us.dison.compactmachines.*

import scala.collection.mutable.HashMap
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.registry.RegistryKey

import java.util
import scala.collection.mutable
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
class ActiveWorldFields(var level: World = null) extends PersistentState {
  private val fields = mutable.HashMap[BlockPos, IMiniaturizationField]()

  def setLevel(level: World): Unit = this.level = level

  def getFields: List[IMiniaturizationField] = fields.values.toList

  def tickFields(): Unit = {
    val loaded = fields.values.filter(_.isLoaded()).toSet

    loaded.foreach(_.tick())
  }

  def addFieldInstance(field: IMiniaturizationField): Option[IMiniaturizationField] = {
    field.setWorld(level)

    val center = field.getCenter

    fields.put(center, field)
  }

  def registerField(field: IMiniaturizationField): IMiniaturizationField = {
    val anyMissing = ProjectorBlock.getMissingProjectorsSize(level, field.getFieldSize, field.getCenter).nonEmpty

    if (anyMissing)
      return field

    addFieldInstance(field)
    field.getProjectorPositions.forEach { pos =>
      val stateAt = level.getBlockState(pos)
      // We would have had a missing and returned early if any weren't a projector block
      assume(stateAt.getBlock.isInstanceOf[ProjectorBlock], "Should have returned early")
      if (stateAt.hasBlockEntity) {
        val tileAt = level.getBlockEntity(pos)
        tileAt match {
          case e: ProjectorBlockEntity =>
            e.fieldRef = Option(field)
          case _ =>
            CompactMachines.LOGGER.error("Entity at projector block isn't a projector block entity!")
        }
      }
    }

    field
  }

  def unregisterField(center: BlockPos): Unit = {
    if (fields.contains(center)) {
      val removed = fields.remove(center).get
      // TODO: networking
      removed.cleanForDisposal()
    }
  }

  def unregisterField(field: IMiniaturizationField): Unit =
    unregisterField(field.getCenter)

  def get(center: BlockPos): Option[IMiniaturizationField] = fields.get(center)

  def hasActiveField(center: BlockPos): Boolean = fields.contains(center)

  def getFields(chunk: ChunkPos): mutable.Iterable[IMiniaturizationField] =
    fields.filter { case (key, _) =>
      ChunkPos(key) == chunk
    }.values

  def levelId: RegistryKey[World] = level.getRegistryKey

  override def writeNbt(tag: NbtCompound): NbtCompound = {
    ActiveWorldFields.codec.encodeStart(NbtOps.INSTANCE, fields.values.toList.asJava)
      .result()
      .ifPresent(goodTag => tag.put("cm_fields", goodTag))
    tag
  }

}

object ActiveWorldFields {
  val key = "compactmachines_fields"
  val codec: Codec[util.List[IMiniaturizationField]] = MiniaturizationField.codec.listOf()

  val fieldManagers: mutable.HashMap[RegistryKey[World], ActiveWorldFields] = mutable.HashMap[RegistryKey[World], ActiveWorldFields]()

  def getOrCreate(world: World): ActiveWorldFields = {
    fieldManagers.get(world.getRegistryKey) match {
      case Some(fm) =>
        fm
      case None =>

        val fm = this.get(world)
        fieldManagers(world.getRegistryKey) = fm
        fm
    }
  }

  def tick(world: World): Unit = {
    getOrCreate(world).tickFields()
  }

  def fromTag(tag: NbtCompound): ActiveWorldFields = {
    val worldFields = ActiveWorldFields()
    codec.parse(NbtOps.INSTANCE, tag.getList("cm_fields", NbtElement.COMPOUND_TYPE))
      .result()
      .ifPresent(f => worldFields.fields ++= Map.from(f.asScala.map(v => (v.getCenter, v))))
    worldFields
  }

  private def get(world: World) = {
    val server = world.asInstanceOf[ServerWorld]
    val fields = server.getPersistentStateManager.getOrCreate[ActiveWorldFields](ActiveWorldFields.fromTag, () => ActiveWorldFields(world), key)
    fields.setLevel(world)
    fields
  }
}