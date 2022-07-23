package us.dison.compactmachines.block.entity 

import net.minecraft.block.BlockState 
import net.minecraft.block.entity.BlockEntity 
import net.minecraft.block.entity.BlockEntityType 
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos 

abstract class AbstractWallBlockEntity(blockEntityType: BlockEntityType[_], pos:BlockPos, state:BlockState) extends BlockEntity(blockEntityType, pos, state): 
  private var parentID_ : Option[Int] = Option.empty

  override def readNbt(tag: NbtCompound) = 
    super.readNbt(tag)
    val parentTag = tag.getInt("parent")
    parentID_ = Option.unless(parentTag == -1)(parentTag)

  override protected def writeNbt(tag: NbtCompound): Unit = 
    tag.putInt("parent", parentID.getOrElse(-1)) 

    super.writeNbt(tag) 

  def parentID : Option[Int] = parentID_

  def setParentID(parentID: Int) = 
    parentID_ = Some(parentID)
    markDirty()


