package us.dison.compactmachines.enums;

import net.minecraft.util.StringIdentifiable 
import com.mojang.serialization.Codec 
import net.minecraft.util.math.Direction 
enum TunnelGoing(val going: String) extends java.lang.Enum[TunnelGoing], StringIdentifiable derives CanEqual {
  case Incoming extends TunnelGoing("incoming")
  case Outgoing extends TunnelGoing("outgoing")
  case Neither extends TunnelGoing("neither")

  def asString() = this.going 
}

object TunnelGoing {
  def byName(name: String) : Option[TunnelGoing] = { 
    for (x <- TunnelGoing.values) {
      if x.going == name then return Some(x) 
    }
    return None
  }
  val CODEC : Codec[TunnelGoing] = StringIdentifiable.createCodec(() => TunnelGoing.values)
}
