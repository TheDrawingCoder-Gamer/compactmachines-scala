package us.dison.compactmachines.util 

import net.minecraft.nbt.NbtCompound 
import net.minecraft.nbt.NbtElement 
import net.minecraft.util.math.BlockPos 
import us.dison.compactmachines.enums.TunnelDirection 
import us.dison.compactmachines.data.persistent.Room
import us.dison.compactmachines.data.persistent.tunnel.Tunnel 
import us.dison.compactmachines.data.persistent.tunnel.TunnelType
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockView
import us.dison.compactmachines.block.entity.MachineBlockEntity
import us.dison.compactmachines.CompactMachines

object TunnelUtil:
  /*
  def typeFromNbt(stackNbt: NbtCompound): Option[TunnelType] =  
    val typeNbt = stackNbt.get("type")
    if typeNbt != null then 
      val strType = typeNbt.asString() 
      return TunnelType.byName(strType)
    end if 
    return None
  */
  def nextSide(room: Room, cur: TunnelDirection): TunnelDirection = 
    val tunneldirections = room.tunnels.filter(_.face != TunnelDirection.NoDir).map(
      _.face)
    val dirs = TunnelDirection.values
    var i = 0 
    var breakOnNext = false 
    // get safety: if it's not there it would have hanged LOL
    // ^ accidental foreshadowing
    List.concat(dirs, dirs).dropWhile((dir: TunnelDirection) => dir != cur).drop(1).find(!tunneldirections.contains(_)).get 
  def rotate(room : Room, t: Tunnel): Tunnel = 
    t.copy(face = nextSide(room, t.face))
  def fromRoomAndPos(room: Room, pos: BlockPos): Option[Tunnel] = 
    for tunnel <- room.tunnels do 
      if equalBlockPos(pos, tunnel.pos) then 
        return Option(tunnel) 
    return None
  def equalBlockPos(a: BlockPos, b: BlockPos): Boolean = 
    a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ()
  def getTunnelOf(direction: Option[Direction], tunnelType: TunnelType, room: Room): Option[Tunnel] = 
    room.tunnels.find(t => 
        val dirEq = t.face.toDirection() == direction 
        CompactMachines.LOGGER.info(t.face.toDirection().toString + " == " + direction.toString + " = " + dirEq.toString) 
        val ttEq = t.tunnelType == tunnelType 
        CompactMachines.LOGGER.info(t.tunnelType.asString() + " == " + tunnelType.asString + " = " + ttEq.toString ) 
        dirEq && ttEq)
  def getTunnelOfMachine(world: BlockView, pos: BlockPos, direction: Option[Direction], tunnelType: TunnelType): Either[String, Tunnel] = 
    world.getBlockEntity(pos) match 
      case null => Left("No block entity at pos") 
      case mBEntity : MachineBlockEntity => 
        mBEntity.machineID.flatMap(CompactMachines.roomManager.getRoomByNumber(_)) match 
          case None => Left("Machine ID is null or room is null") 
          case Some(room) => 
            getTunnelOf(direction, tunnelType, room).toRight("Failed to get tunnel")


