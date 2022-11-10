package us.dison.compactmachines.enums 

import net.minecraft.util.StringIdentifiable 
import com.mojang.serialization.Codec 
import net.minecraft.util.math.Direction 
enum TunnelDirection(val direction: String) extends java.lang.Enum[TunnelDirection], StringIdentifiable derives CanEqual: 
  case North extends TunnelDirection("north")
  case East extends TunnelDirection("east")
  case South extends TunnelDirection("south")
  case West extends TunnelDirection("west")
  case Up extends TunnelDirection("up")
  case Down extends TunnelDirection("down")
  case NoDir extends TunnelDirection("none")
  
  def toDirection() : Option[Direction] = 
    this match 
      case North => Option.apply(Direction.NORTH)
      case East => Option.apply(Direction.EAST) 
      case South => Option.apply(Direction.SOUTH) 
      case West => Option.apply(Direction.WEST) 
      case Up => Option.apply(Direction.UP )
      case Down => Option.apply(Direction.DOWN) 
      case NoDir => None
  end toDirection
  override def asString() = direction
object TunnelDirection: 
  def byName(name: String) : Option[TunnelDirection] = 
    for x <- TunnelDirection.values do 
      if x.direction == name then return Some(x) 
    end for
    return None
  end byName
  val CODEC : Codec[TunnelDirection] = StringIdentifiable.createCodec(() => TunnelDirection.values, TunnelDirection.byName(_).getOrElse(null))
end TunnelDirection



