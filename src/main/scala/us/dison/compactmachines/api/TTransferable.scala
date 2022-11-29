package us.dison.compactmachines.api

import net.minecraft.server.world.ChunkTicketType
import net.minecraft.util.Identifier
import net.minecraft.util.math.{BlockPos, ChunkPos, Direction}
import net.minecraft.util.registry.{Registry, RegistryKey}
import net.minecraft.world.World
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.block.entity.TunnelWallBlockEntity
import us.dison.compactmachines.data.persistent.tunnel.TunnelType

extension[T] (it : ITransferable[T]) {
  def name : Identifier = it.getName
  def internalTarget(wall : TunnelWallBlockEntity) : Option[T] = {
    wall.tunnelType match {
      case Some(TunnelType.Normal) => 
        try {
          Direction.values.collectFirst(Function.unlift { (dir : Direction) => 
            val newPos = wall.getPos.offset(dir, 1)
            wall.getWorld.getBlockEntity(newPos) match {
              case _ : TunnelWallBlockEntity => None 
              case be => 
                val storage = it.lookup(wall.getWorld, newPos, dir)
                Option(storage)
            }
          }
          )
        } catch {
          case e : Exception => 
            CompactMachines.LOGGER.warn("Suppressed Exception while trying to get internal target")
            CompactMachines.LOGGER.warn(e)
            None
        }
      case _ => None 
    }
  }
  def externalTarget(wall : TunnelWallBlockEntity) : Option[T] = {
    wall.tunnelType match {
      case Some(TunnelType.Normal) => 
            wall.room.flatMap { room => 
              val machinePos = room.machine 
              try {
                val machineWorld = wall.getWorld.getServer.getWorld(RegistryKey.of(Registry.WORLD_KEY,  room.world))
                wall.tunnel.flatMap(_.face.toDirection).flatMap { (dir : Direction) => 
                  val targetPos = machinePos.offset(dir, 1)
                  machineWorld.getChunkManager.addTicket(ChunkTicketType.PORTAL, ChunkPos(targetPos), 3, targetPos)
                  Option(it.lookup(machineWorld, targetPos, dir))
                }
              } catch {
                case e : Exception => 
                  CompactMachines.LOGGER.warn("Suppressed Exception while trying to get external target")
                  CompactMachines.LOGGER.warn(e)
                  None
              }
            }
      case _ => None 
    }
  }
}
