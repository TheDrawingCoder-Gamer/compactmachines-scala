package us.dison.compactmachines.data.persistent 

import com.mojang.serialization.Codec
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtOps
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.{BlockPos, Direction}
import net.minecraft.world.PersistentState
import net.minecraft.world.World
import us.dison.compactmachines.data.persistent.tunnel.Tunnel
import us.dison.compactmachines.util.TunnelUtil

import java.util.Collections
import scala.collection.mutable.{Buffer, ListBuffer}
import scala.jdk.CollectionConverters.*
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.util.RoomUtil

import java.util.UUID
import scala.collection.mutable
object RoomManager {
  val CODEC: Codec[java.util.List[Room]] = Codec.list(Room.CODEC)
  val KEY : String = "compactmachines_rooms"

  def getCenterPosByID(id : Int) : BlockPos = {
    val pos = BlockPos.Mutable(255 + 8, 7, 255 + 8)
    var direction = Direction.NORTH

    var index = 1

    for (i <- 2 to id) {
      if (index != id) {
        val curLegLength : Int = i / 2
        for (legIndex <- 0 until curLegLength) {
          pos.move(direction, 16)
          index += 1
        }
        direction = direction.rotateCounterclockwise(Direction.Axis.Y)
      }
    }
    pos.toImmutable
  }
  def fromTag(tag: NbtCompound): RoomManager =
    val roomManager = RoomManager()
    roomManager.rooms.clear()

    val newRooms: mutable.Buffer[Room] = CODEC.parse(NbtOps.INSTANCE, tag.getList("rooms", NbtElement.COMPOUND_TYPE))
                                              .result()
                                              .orElse(Collections.emptyList).asScala
    roomManager.rooms.addAll(newRooms)
    roomManager

  def get(world: World): RoomManager =
    val serverWorld = world.asInstanceOf[ServerWorld]
    serverWorld.getPersistentStateManager.nn.getOrCreate(RoomManager.fromTag, () => new RoomManager, KEY).nn
}
class RoomManager extends PersistentState: 
  private val rooms : ListBuffer[Room] = ListBuffer()
  
  override def writeNbt(tag : NbtCompound): NbtCompound = 
    RoomManager.CODEC.encodeStart(NbtOps.INSTANCE, rooms.asJava)
      .result()
      .ifPresent(goodTag => tag.put("rooms", goodTag))
    tag
  def onServerWorldLoad(world: ServerWorld): Unit = {}
  def onServerWorldTick(world: ServerWorld): Unit = {}
  def addRoom(room: Room): Unit = 
    require(!existsRoomByNumber(room.number), s"Room already exists with number: ${room.number.toString}")
    rooms.addOne(room)
    markDirty()

  def updateOwner(id: Int, uuid: String): Unit = 
    getRoomByNumber(id) match 
      case Some(oldRoom) => 
        val newRoom = oldRoom.copy(owner = uuid) 
        rooms.subtractOne(oldRoom)
        rooms.addOne(newRoom)
        markDirty()
      case None => ()

  def updateMachinePos(id: Int, world: Identifier, machine: BlockPos): Unit = 
    getRoomByNumber(id) match 
      case Some(oldRoom) =>
        val newRoom = oldRoom.copy(world = world, machine = machine)
        rooms.subtractOne(oldRoom)
        rooms.addOne(newRoom)
        markDirty()
      case None => ()

  def updateMachinePosAndOwner(id: Int, world: Identifier, machine: BlockPos, uuid: String): Unit = 
    getRoomByNumber(id)  match 
      case Some(oldRoom) => 
        val newRoom = oldRoom.copy(world = world, machine = machine, owner = uuid)
        rooms.subtractOne(oldRoom)
        rooms.addOne(newRoom)
        markDirty()
      case None => 
        ()
  def updateSpawnPos(id : Int, pos: BlockPos): Unit = 
    getRoomByNumber(id) match 
      case Some(oldRoom) =>
        val newRoom = oldRoom.copy(spawnPos = pos)
        rooms.subtractOne(oldRoom)
        rooms.addOne(newRoom)
      case None => ()
  def rmPlayer(id: Int, playerUuid: UUID): Unit = 
    val player = playerUuid.toString
    getRoomByNumber(id) match {
      case Some(oldRoom) => 
        if !oldRoom.players.contains(player) then 
          CompactMachines.LOGGER.warn("tried to remove player from room, but they aren't in it")
        else 
          updatePlayersByString(id, oldRoom.players.filterNot(_ == player))
      case None => ()
    }
  def addPlayer(id: Int, playerUuid: UUID): Unit = 
    val player = playerUuid.toString
    getRoomByNumber(id) match {
      case Some(oldRoom) => 
        if oldRoom.players.contains(player) then 
          CompactMachines.LOGGER.warn("tried to put player in room, but they were already in it")
        else 
          updatePlayersByString(id, oldRoom.players.appended(player))
      case None => ()
    }
  def updatePlayers(id: Int, players: IterableOnce[UUID]): Unit = updatePlayersByString(id, players.iterator.map(_.toString))
  def updatePlayersByString(id: Int, players: IterableOnce[String]): Unit =
    getRoomByNumber(id) match 
      case Some(oldRoom) => 
        val playerList = List.from(players)
        val newRoom = oldRoom.copy(players = playerList)
        rooms.subtractOne(oldRoom) 
        rooms.addOne(newRoom)
      case None => ()
  
  def updateTunnels(id: Int, tunnels: IterableOnce[Tunnel]): Unit = 
    getRoomByNumber(id) match 
      case Some(oldRoom) => 
        val newRoom = oldRoom.copy(tunnels = List.from(tunnels)) 
        rooms.subtractOne(oldRoom)
        rooms.addOne(newRoom)
        markDirty()
      case None => ()
  def rmTunnel(id: Int, tunnel: Tunnel): Unit = 
    val oldRoom = getRoomByNumber(id)
    if oldRoom.isEmpty then return 
    updateTunnels(id, oldRoom.get.tunnels.filter(_ != tunnel))
  def addTunnel(id: Int, tunnel: Tunnel): Unit = 
    val oldRoom = getRoomByNumber(id) 
    if oldRoom.isEmpty then return 
    updateTunnels(id, oldRoom.get.tunnels.appended(tunnel))
  def updateTunnel(id: Int, tunnel: Tunnel) : Unit = 
    (getRoomByNumber(id) : Option[Room]) match 
      case None => 
        CompactMachines.LOGGER.warn("Can't update tunnel in an invalid room")
      case Some(oldRoom) => 
        val tunnels = oldRoom.tunnels 
        if tunnels.size < 1 then 
          addTunnel(id, tunnel) 
        else 
          val pos = tunnel.pos 
          tunnels.find((t: Tunnel) => TunnelUtil.equalBlockPos(tunnel.pos, t.pos)) match 
            case Some(oldTunnel) => 
              updateTunnels(id,tunnels.filter(_ != oldTunnel).appended(tunnel))
            case None => ()
  def getRoomByNumber(id: Int):Option[Room] =
   rooms.find(_.number == id)

  def getRoomFromPos(pos: BlockPos):Option[Room] = 
    rooms.find { room =>
      val box = RoomUtil.getBox(RoomManager.getCenterPosByID(room.number), 13)
      box.contains(pos.getX(), pos.getY(), pos.getZ())

    }
  def existsRoomByNumber(id:Int):Boolean = {
    rooms.exists(_.number == id)
  }
  def nextID : Int = {
    var i = 1
    while (existsRoomByNumber(i)) {
      i += 1
    }
    i
  }

