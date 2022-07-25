package us.dison.compactmachines.block.entity 


import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import us.dison.compactmachines.CompactMachines;
import us.dison.compactmachines.enums.MachineSize;
import us.dison.compactmachines.data.persistent.Room;
import us.dison.compactmachines.data.persistent.RoomManager;
import us.dison.compactmachines.util.RoomUtil;
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
    if this.owner.isEmpty then setOwner(UUID(0, 0))
    tag.putUuid("uuid", this.owner.getOrElse(UUID(0, 0))) 
    tag.putInt("parentID", this.parentID.getOrElse(-1))
    super.writeNbt(tag)
  override def toUpdatePacket() = 
    BlockEntityUpdateS2CPacket.create(this) 
  override def toInitialChunkDataNbt() = 
    createNbt() 
  def machineID = machineID_ 
  def setMachineID(machineID : Int) = 
    this.machineID_ = Some(machineID)
    markDirty() 
  def owner = owner_
  def setOwner(owner : UUID) = 
    this.owner_ = Option(owner) 
    markDirty()
  def parentID = parentID_ 
  def setParentID(parentID : Option[Int]) = 
    this.parentID_ = parentID 
    markDirty()
object MachineBlockEntity extends Ticker[MachineBlockEntity]: 
  def tick(world: World, pos: BlockPos, state: BlockState, machineBlock: MachineBlockEntity): Unit = 
    if world.isClient() then return 
    
    val now = world.getServer().getTicks()
    if now - machineBlock.lastPlayerCheckTick.getOrElse(-1) < 10 then return 
    machineBlock.lastPlayerCheckTick = Option(now) 

    // imagine using null unironically
    machineBlock.machineID match 
      case Some(id) =>
        val box = RoomUtil.getBox(RoomUtil.getCenterPosByID(id), machineBlock.size.size)
        
        val players = world.getServer().getWorld(CompactMachines.CMWORLD_KEY).getPlayers.nn

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

