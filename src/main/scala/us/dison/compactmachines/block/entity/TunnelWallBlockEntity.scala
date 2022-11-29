package us.dison.compactmachines.block.entity

import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.Packet
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Direction
import net.minecraft.util.registry.Registry
import net.minecraft.util.registry.RegistryKey
import net.minecraft.world.World
import team.reborn.energy.api.EnergyStorage
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.enums.{TunnelDirection, TunnelGoing}
import us.dison.compactmachines.data.persistent.Room
import us.dison.compactmachines.data.persistent.tunnel.Tunnel
import us.dison.compactmachines.data.persistent.tunnel.TunnelType
import us.dison.compactmachines.util.TunnelUtil
import us.dison.compactmachines.block.TunnelWallBlock
import us.dison.compactmachines.block.entity.AbstractWallBlockEntity
import us.dison.compactmachines.util.RoomUtil

//noinspection UnstableApiUsage
class TunnelWallBlockEntity(pos: BlockPos, state: BlockState) extends BlockEntity(CompactMachines.TUNNEL_WALL_BLOCK_ENTITY: @unchecked, pos, state), AbstractWallBlockEntity, RenderAttachmentBlockEntity:
  private var tunnelTypeVar : Option[TunnelType] = None
  private var connectedToItemVar = false 
  private var connectedToEnergyVar = false 
  private var connectedToFluidVar = false 
  private var outgoingVar = false
  override def readNbt(tag: NbtCompound): Unit =
    super.readNbt(tag) 
    this.tunnelTypeVar = Option(tag.getString("type")).map((strType : String | Null) => TunnelType.byName(strType.nn)).flatten
    this.connectedToItemVar = Option(tag.getBoolean("connectedToItem")).getOrElse(false)
    this.connectedToFluidVar = Option(tag.getBoolean("connectedToFluid")).getOrElse(false)
    this.connectedToEnergyVar = Option(tag.getBoolean("connectedToEnergy")).getOrElse(false)
    this.outgoingVar = Option(tag.getBoolean("outgoing")).getOrElse(false)

  override protected[entity] def writeNbt(tag: NbtCompound): Unit =
    tag.putBoolean("connectedToItem", this.connectedToItem)
    tag.putBoolean("connectedToEnergy", this.connectedToEnergy)
    tag.putBoolean("connectedToFluid", this.connectedToFluid) 
    tag.putString("type", this.tunnelType.map(_.asString()).getOrElse("")) 
    tag.putBoolean("outgoing", this.outgoing) 

    super.writeNbt(tag) 
  def isConnected: Boolean =
    connectedToFluid || connectedToEnergy || connectedToItem
  override def getRenderAttachmentData: Object | Null =
    TunnelRenderAttachmentData(tunnelType, isConnected, outgoing) 
  override def toInitialChunkDataNbt: NbtCompound =
    createNbt() 
  def tunnelType: Option[TunnelType] = tunnelTypeVar
  def tunnelType_=(tunnelType: Option[TunnelType]): Unit =
    this.tunnelTypeVar = tunnelType
    markDirty() 

  def connectedToFluid: Boolean = connectedToFluidVar
  def connectedToFluid_=(connected:Boolean): Unit =
      connectedToFluidVar = connected 
      (this.room, this.tunnel) match 
          case (Some(room), Some(tunnel)) => 
            CompactMachines.roomManager.updateTunnel(room.number, tunnel.copy(connectedToFluid = connected))
            markDirty()
          case _ => ()
  def connectedToItem: Boolean = connectedToItemVar
  def connectedToItem_=(connected:Boolean): Unit =
      connectedToItemVar = connected
      (this.room, this.tunnel) match 
        case (Some(room), Some(tunnel)) =>
          CompactMachines.roomManager.updateTunnel(room.number, tunnel.copy(connectedToItem = connected))
          markDirty()
        case _ => ()
  def connectedToEnergy: Boolean = connectedToEnergyVar
  def connectedToEnergy_=(connected:Boolean): Unit =
      connectedToEnergyVar = connected 
      (this.room, this.tunnel) match 
        case (Some(room), Some(tunnel)) =>
          CompactMachines.roomManager.updateTunnel(room.number, tunnel.copy(connectedToEnergy = connected))
          markDirty()
        case _ => () 
  def outgoing: Boolean = outgoingVar
  def outgoing_=(outgoing: Boolean): Unit =
    this.outgoingVar = outgoing 
    (this.room, this.tunnel) match 
      case (Some(room), Some(tunnel)) =>
        CompactMachines.roomManager.updateTunnel(room.number, tunnel.copy(outgoing = outgoing)) 
        markDirty() 
      case _ => ()
  def room : Option[Room] = 
    parentID.flatMap(CompactMachines.roomManager.getRoomByNumber(_))
  def tunnel : Option[Tunnel] = 
    this.room.flatMap(TunnelUtil.fromRoomAndPos(_, this.getPos))
  def tunnel_=(tunnel: Tunnel): Unit = 
    parentID.foreach { id =>
      CompactMachines.LOGGER.info("updating tunnel")
      CompactMachines.roomManager.updateTunnel(id, tunnel)
      this.tunnelTypeVar = Some(tunnel.tunnelType)
      this.outgoingVar = tunnel.outgoing
      this.connectedToEnergyVar = tunnel.connectedToEnergy
      this.connectedToItemVar = tunnel.connectedToItem
      this.connectedToFluidVar = tunnel.connectedToFluid
      this.world.setBlockState(this.getPos, this.world.getBlockState(this.getPos)
        .`with`(TunnelWallBlock.CONNECTED_SIDE, tunnel.face)
        .`with`(TunnelWallBlock.GOING, if (tunnel.tunnelType == TunnelType.Redstone) {
          if (this.outgoing) TunnelGoing.Outgoing else TunnelGoing.Incoming
        } else TunnelGoing.Neither)
        .`with`(TunnelWallBlock.TUNNEL_TYPE, tunnel.tunnelType)
      )
      markDirty()
    }
  private def externalHelper[T](lookup: (World, BlockPos, Direction) =>  T): Option[T] = 
    tunnelType match 
      case Some(TunnelType.Normal) =>
        this.room.flatMap { room =>
          val machinePos = room.machine
          try {
            val machineWorld = getWorld.getServer.getWorld(RegistryKey.of(Registry.WORLD_KEY, room.world))
            this.tunnel.flatMap(_.face.toDirection).flatMap { dir =>
              val targetPos = machinePos.offset(dir, 1)
              machineWorld.getChunkManager.addTicket(ChunkTicketType.PORTAL, ChunkPos(targetPos), 3, targetPos)
              Option(lookup(machineWorld, targetPos, dir))
            }
          } catch {
            case ignored: Exception =>
              CompactMachines.LOGGER.error("Suppressed exception while trying to get external target")
              CompactMachines.LOGGER.error(ignored)
              None
          }
        }
      case _ => None
  private def internalHelper[T](lookup: (World, BlockPos, Direction) => T): Option[T] = 
    tunnelType match 
      case Some(TunnelType.Normal) =>
        try { 
          Direction.values.collectFirst(Function.unlift { (dir: Direction) =>
            val newPos = pos.offset(dir, 1)
            world.getBlockEntity(newPos) match
              case _: TunnelWallBlockEntity => None
              case be =>
                val storage = lookup(world, newPos, dir)
                // Option checks for null
                Option(storage)
          })
        } catch {
          case ignored: Exception => 
            CompactMachines.LOGGER.error("Suppressed exception while trying to get internal target")
            CompactMachines.LOGGER.error(ignored) 
            None
        }
      case _ => None
  def extItemTarget : Option[Storage[ItemVariant]] = 
    externalHelper(ItemStorage.SIDED.find)
  def intlItemTarget : Option[Storage[ItemVariant]] =
    internalHelper(ItemStorage.SIDED.find)
  def extFluidTarget : Option[Storage[FluidVariant]] = 
    externalHelper(FluidStorage.SIDED.find)
  def intlFluidTarget : Option[Storage[FluidVariant]] =
    internalHelper(FluidStorage.SIDED.find)
  def extEnergyTarget : Option[EnergyStorage] = 
    externalHelper(EnergyStorage.SIDED.find)
  def intlEnergyTarget : Option[EnergyStorage] =
    internalHelper(EnergyStorage.SIDED.find)
  def getMachineEntity: Option[MachineBlockEntity] = 
    try {
      this.room.flatMap { room =>
        val machineWorld = this.getWorld.getServer.getWorld(RegistryKey.of(Registry.WORLD_KEY, room.world))
        machineWorld.getChunkManager.addTicket(ChunkTicketType.PORTAL, ChunkPos(room.machine), 3, room.machine)
        Option(machineWorld.getBlockEntity(room.machine).asInstanceOf[MachineBlockEntity])
      }
    } catch {
      case e: Exception => 
        CompactMachines.LOGGER.error("Suppressed exception while fetching machine entity")
        CompactMachines.LOGGER.error(e)
        None
    }
object TunnelWallBlockEntity extends us.dison.compactmachines.util.Ticker[TunnelWallBlockEntity]:
  def tick(world: World, blockPos: BlockPos, blockState: BlockState, tunnelBlockEntity: TunnelWallBlockEntity): Unit = ()
case class TunnelRenderAttachmentData(tunnelType: Option[TunnelType], isConnected: Boolean, isOutgoing: Boolean)
