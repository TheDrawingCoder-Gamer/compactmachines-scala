package us.dison.compactmachines.block.entity 


import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.Packet
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World

import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.enums.MachineSize
import us.dison.compactmachines.data.persistent.Room
import us.dison.compactmachines.data.persistent.RoomManager
import us.dison.compactmachines.util.RoomUtil
import us.dison.compactmachines.util.Ticker

import scala.jdk.CollectionConverters._
import java.util.UUID
import scala.collection.mutable.ListBuffer
class MachineBlockEntity(
  pos: BlockPos, 
  state: BlockState, 
  val size: Option[MachineSize], 
  private var parentID_ : Option[Int]) extends BlockEntity(CompactMachines.MACHINE_BLOCK_ENTITY : @unchecked, pos, state):
  private var machineID_ = Option.empty[Int] 
  private var lastPlayerCheckTick = Option.empty[Int] 
  private var owner_ = Option.empty[UUID]
  override def readNbt(tag: NbtCompound): Unit = 
    super.readNbt(tag) 
    val machineTag = tag.getInt("number")
    this.machineID_ = Option.unless(machineTag == -1)(machineTag) 
    this.owner_ = Option(tag.getUuid("uuid"))
    val parentTag = tag.getInt("parentID")
    this.parentID_ = Option.unless(parentTag == -1)(parentTag)
  override protected[entity] def writeNbt(tag: NbtCompound): Unit = 
    tag.putInt("number", this.machineID.getOrElse(-1))
    if this.owner.isEmpty then owner = Some(UUID(0, 0))
    tag.putUuid("uuid", this.owner.getOrElse(UUID(0, 0))) 
    tag.putInt("parentID", this.parentID.getOrElse(-1))
    super.writeNbt(tag)
  override def toUpdatePacket: Packet[ClientPlayPacketListener] =
    BlockEntityUpdateS2CPacket.create(this) 
  override def toInitialChunkDataNbt: NbtCompound =
    createNbt() 
  def machineID: Option[Int] = machineID_
  def machineID_=(machineID : Option[Int]): Unit =
    this.machineID_ = machineID
    markDirty() 
  def owner: Option[UUID] = owner_
  def owner_=(owner : Option[UUID]): Unit =

    this.owner_ = owner
    markDirty()
  def parentID: Option[Int] = parentID_
  def parentID_=(parentID : Option[Int]): Unit =
    this.parentID_ = parentID 
    markDirty()
object MachineBlockEntity extends Ticker[MachineBlockEntity]: 
  def tick(world: World, pos: BlockPos, state: BlockState, machineBlock: MachineBlockEntity): Unit = 
    if world.isClient then return
    
    val now = world.getServer.getTicks
    if now - machineBlock.lastPlayerCheckTick.getOrElse(-1) < 10 then return 
    machineBlock.lastPlayerCheckTick = Option(now) 

    // imagine using null unironically
    machineBlock.machineID match 
      case Some(id) =>
        val box = RoomUtil.getBox(RoomManager.getCenterPosByID(id), machineBlock.size.size)
        
        val players = world.getServer.getWorld(CompactMachines.CMWORLD_KEY).getPlayers.nn

        val roomManager = CompactMachines.roomManager 
        roomManager.getRoomByNumber(machineBlock.machineID.getOrElse(-1)) match 
          case Some(room) => 
            if players.size > 0 then 
              val playersInMachine = ListBuffer[ServerPlayerEntity]() 
              players.asScala.foreach(player => 
              if box.contains(player.getPos()) then 
                playersInMachine.addOne(player) 
              )
              if room.players.length != playersInMachine.length then 
                roomManager.updatePlayers(machineBlock.machineID.getOrElse(-1), playersInMachine.map(_.getUuid))
            else 
              roomManager.updatePlayers(machineBlock.machineID.getOrElse(-1), List())
          case None => 
            ()
      case None => ()
  end tick 

