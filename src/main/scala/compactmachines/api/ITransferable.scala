package us.dison.compactmachines.api;

import net.minecraft.world.World
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import us.dison.compactmachines.data.persistent.tunnel.TunnelType
import us.dison.compactmachines.block.entity.TunnelWallBlockEntity
import us.dison.compactmachines.CompactMachines
import net.minecraft.util.registry.Registry
import net.minecraft.util.registry.RegistryKey
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.Identifier

trait ITransferable[T] {
  def name : Identifier
  protected def lookup(world : World, pos: BlockPos, direction: Direction) : T
  final def internalTarget(wall : TunnelWallBlockEntity) : Option[T] = {
    wall.tunnelType match {
      case Some(TunnelType.Normal) => 
        try {
          Direction.values.collectFirst(Function.unlift { (dir : Direction) => 
            val newPos = wall.getPos().offset(dir, 1)
            wall.getWorld().getBlockEntity(newPos) match {
              case _ : TunnelWallBlockEntity => None 
              case be => 
                val storage = lookup(wall.getWorld(), newPos, dir)
                Option(storage)
            }
          }
          )
        } catch {
          case e : Exception => 
            CompactMachines.LOGGER.warn("Surpressed Exception while trying to get internal target")
            CompactMachines.LOGGER.warn(e)
            None
        }
      case _ => None 
    }
  }
  final def externalTarget(wall : TunnelWallBlockEntity) : Option[T] = {
    wall.tunnelType match {
      case Some(TunnelType.Normal) => 
            wall.room.flatMap { room => 
              val machinePos = room.machine 
              try {
                val machineWorld = wall.getWorld.getServer().getWorld(RegistryKey.of(Registry.WORLD_KEY,  room.world))
                wall.tunnel.flatMap(_.face.toDirection()).flatMap { (dir : Direction) => 
                  val targetPos = machinePos.offset(dir, 1)
                  machineWorld.getChunkManager().addTicket(ChunkTicketType.PORTAL, ChunkPos(targetPos), 3, targetPos)
                  Option(lookup(machineWorld, targetPos, dir))
                }
              } catch {
                case e : Exception => 
                  CompactMachines.LOGGER.warn("Surpressed Exception while trying to get external target")
                  CompactMachines.LOGGER.warn(e)
                  None
              }
            }
      case _ => None 
    }
  }
}
