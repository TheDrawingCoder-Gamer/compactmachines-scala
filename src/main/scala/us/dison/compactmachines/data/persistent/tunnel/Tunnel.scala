package us.dison.compactmachines.data.persistent.tunnel

import com.mojang.serialization.Codec 
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.math.BlockPos 
import us.dison.compactmachines.enums.TunnelDirection
import us.dison.compactmachines.util.codecs.* 
case class Tunnel(pos : BlockPos, 
             face: TunnelDirection, 
             tunnelType: TunnelType, 
             connectedToItem: Boolean, 
             connectedToFluid: Boolean, 
             connectedToEnergy: Boolean,
             outgoing: Boolean) derives CanEqual:
    override def toString(): String = 
      String.format("Tunnel { pos: %s, face: %s, type: %s, connectedToItem: %b, connectedToFluid: %b, connectedToEnergy: %b, outgoing: %b}",
        pos.toString(), face.toString(), tunnelType.toString(), connectedToItem, connectedToFluid, connectedToEnergy, outgoing)
    override def canEqual(x: Any): Boolean = 
      x.isInstanceOf[Tunnel]
    override def equals(x: Any): Boolean = 
      if !canEqual(x) then return false
      val y = x.asInstanceOf[Tunnel]
      return y.pos.getX() == pos.getX() 
         && y.pos.getY() == pos.getY() 
         && y.pos.getZ() == pos.getZ() 
         && y.tunnelType == tunnelType 
         && y.face == face 
         && y.connectedToItem == connectedToItem 
         && y.connectedToFluid == connectedToFluid 
         && y.connectedToEnergy == connectedToEnergy 
         && y.outgoing == outgoing
         
object Tunnel: 
  val CODEC: Codec[Tunnel] = RecordCodecBuilder.create(instance => 
      instance.group(
        BlockPos.CODEC.fieldOf("pos").forGetter((t: Tunnel) => t.pos),
        TunnelDirection.CODEC.fieldOf("face").forGetter((t: Tunnel) => t.face),
        TunnelType.CODEC.fieldOf("type").forGetter((t: Tunnel) => t.tunnelType),
        ScalaBooleanCodec.fieldOf("connectedToFluid").forGetter((t: Tunnel) => t.connectedToFluid),
        ScalaBooleanCodec.fieldOf("connectedToItem").forGetter((t: Tunnel) => t.connectedToItem),
        ScalaBooleanCodec.fieldOf("connectedToEnergy").forGetter((t: Tunnel) => t.connectedToEnergy),
        ScalaBooleanCodec.fieldOf("outgoing").forGetter((t: Tunnel) => t.outgoing))
        .apply(instance, Tunnel.apply))
