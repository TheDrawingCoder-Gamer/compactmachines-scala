package us.dison.compactmachines.util 

import net.minecraft.nbt.NbtCompound 
import net.minecraft.nbt.NbtElement 
import net.minecraft.util.math.BlockPos 
import us.dison.compactmachines.enums.TunnelDirection 
import us.dison.compactmachines.data.persistent.Room
import us.dison.compactmachines.data.persistent.tunnel.Tunnel 
import us.dison.compactmachines.data.persistent.tunnel.TunnelType

object TunnelUtil: 
  def typeFromNbt(stackNbt: NbtCompound): Option[TunnelType] =  
    val typeNbt = stackNbt.get("type")
    if typeNbt != null then 
      val strType = typeNbt.asString() 
      return TunnelType.byName(strType)
    end if 
    return None
  def nextSide(room: Room, cur: TunnelDirection): TunnelDirection = 
    val tunneldirections = room.tunnels.filter(_.face == TunnelDirection.NoDir).map(
      _.face)
    val dirs = TunnelDirection.values
    var i = 0 
    var breakOnNext = false 
    // get safety: if it's not there it would have hanged LOL
    LazyList.continually(dirs).flatten.dropWhile((dir: TunnelDirection) => dir.toDirection() != cur).find(!dirs.contains(_)).get 
  def rotate(room : Room, t: Tunnel): Tunnel = 
    t.copy(face = nextSide(room, t.face))
  def fromRoomAndPos(room: Room, pos: BlockPos): Option[Tunnel] = 
    for tunnel <- room.tunnels do 
      if equalBlockPos(pos, tunnel.pos) then 
        return Option(tunnel) 
    return None
  def equalBlockPos(a: BlockPos, b: BlockPos): Boolean = 
    a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ()
      


