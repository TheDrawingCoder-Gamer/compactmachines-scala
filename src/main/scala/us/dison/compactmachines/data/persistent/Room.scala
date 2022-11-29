package us.dison.compactmachines.data.persistent

import com.mojang.serialization.Codec 
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.Identifier 
import net.minecraft.util.math.BlockPos 
import us.dison.compactmachines.data.persistent.tunnel.Tunnel
import scala.jdk.CollectionConverters._
import us.dison.compactmachines.util.codecs.*
import java.util.UUID
import net.minecraft.util.dynamic.DynamicSerializableUuid
case class Room(world: Identifier,
                owner: String,
                machine: BlockPos, 
                center: BlockPos,
                spawnPos: BlockPos,
                number: Int,
                players: List[String],
                tunnels: List[Tunnel]) derives CanEqual

object Room: 
  val CODEC : Codec[Room] = RecordCodecBuilder.create(instance => 
      instance.group(
        Identifier.CODEC.fieldOf("world").forGetter((room: Room) => room.world),
        Codec.STRING.fieldOf("owner").forGetter((room: Room) => room.owner),
        BlockPos.CODEC.fieldOf("machine").forGetter((room: Room) => room.machine),
        BlockPos.CODEC.fieldOf("center").forGetter((room: Room) => room.center),
        BlockPos.CODEC.fieldOf("spawnPos").forGetter((room: Room) => room.spawnPos),
        ScalaIntCodec.fieldOf("number").forGetter((room: Room) => room.number),
        ScalaListCodec(Codec.STRING).fieldOf("players").forGetter((room: Room) => room.players),
        ScalaListCodec(Tunnel.CODEC).fieldOf("tunnels").forGetter((room: Room) => room.tunnels)
        ).apply(instance, Room.apply))
  
  
