package us.dison.compactmachines.block.entity 

import net.minecraft.block.BlockState 
import net.minecraft.block.entity.BlockEntity 
import net.minecraft.block.entity.BlockEntityType 
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos 

trait AbstractWallBlockEntity extends BlockEntity:
  private var parentID_ : Option[Int] = Option.empty

  override def readNbt(tag: NbtCompound) : Unit =
    super.readNbt(tag)
    val parentTag = tag.getInt("parent")
    parentID = Option.unless(parentTag == -1)(parentTag)

  override protected def writeNbt(tag: NbtCompound): Unit = 
    tag.putInt("parent", parentID.getOrElse(-1)) 

    super.writeNbt(tag) 

  def parentID : Option[Int] = parentID_

  def parentID_=(parentID: Option[Int]) : Unit =
    parentID_ = parentID
    markDirty()


