package us.dison.compactmachines.data.persistent.tunnel

import com.mojang.serialization.Codec
import net.minecraft.util.StringIdentifiable 

enum TunnelType(val tunnelName: String, val id: Int, val color: Int) extends  java.lang.Enum[TunnelType], StringIdentifiable derives CanEqual:
  case Normal extends TunnelType("normal", 0, 0xf5aa42) 
  case Redstone extends TunnelType("redstone", 1, 0xaa1111)

  override def asString() = tunnelName
  
  
object TunnelType:
  val CODEC : Codec[TunnelType] = StringIdentifiable.createCodec[TunnelType](() => TunnelType.values.asInstanceOf[Array[TunnelType | Null] | Null]).nn


  def byID(id: Int) : Option[TunnelType] = {
    for x <- TunnelType.values do 
      if x.id == id then return Option.apply(x)
    end for
    return Option.empty
  } 
  def byName(name: String) : Option[TunnelType] = 
    for x <- TunnelType.values do 
      if x.tunnelName == name then return Option.apply(x) 
    end for 
    return Option.empty
  end byName

