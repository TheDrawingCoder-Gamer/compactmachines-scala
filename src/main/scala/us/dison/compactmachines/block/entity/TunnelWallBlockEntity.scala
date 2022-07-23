package us.dison.compactmachines.block.entity

import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import team.reborn.energy.api.EnergyStorage;

import us.dison.compactmachines.CompactMachines;
import us.dison.compactmachines.enums.TunnelDirection;
import us.dison.compactmachines.data.persistent.Room;
import us.dison.compactmachines.data.persistent.tunnel.Tunnel;
import us.dison.compactmachines.data.persistent.tunnel.TunnelType;
import us.dison.compactmachines.util.TunnelUtil;
import us.dison.compactmachines.block.entity.AbstractWallBlockEntity

class TunnelWallBlockEntity(pos: BlockPos, state: BlockState) extends AbstractWallBlockEntity(CompactMachines.TUNNEL_WALL_BLOCK_ENTITY: @unchecked, pos, state), RenderAttachmentBlockEntity:
  private var tunnelTypeVar : Option[TunnelType] = Option.empty
  private var connectedToItemVar = false 
  private var connectedToEnergyVar = false 
  private var connectedToFluidVar = false 
  private var outgoingVar = false 

  override def readNbt(tag: NbtCompound) = 
    super.readNbt(tag) 
    this.tunnelTypeVar = Option(tag.getString("type")).map((strType : String | Null) => TunnelType.byName(strType.nn)).flatten
    this.connectedToItemVar = Option(tag.getBoolean("connectedToItem")).getOrElse(false)
    this.connectedToFluidVar = Option(tag.getBoolean("connectedToFluid")).getOrElse(false)
    this.connectedToEnergyVar = Option(tag.getBoolean("connectedToEnergy")).getOrElse(false)
    this.outgoingVar = Option(tag.getBoolean("outgoing")).getOrElse(false)

  override protected[entity] def writeNbt(tag: NbtCompound) = 
    tag.putBoolean("connectedToItem", this.connectedToItem)
    tag.putBoolean("connectedToEnergy", this.connectedToEnergy)
    tag.putBoolean("connectedToFluid", this.connectedToFluid) 
    tag.putString("type", this.tunnelType.map(_.asString()).getOrElse("")) 
    tag.putBoolean("outgoing", this.outgoing) 

    super.writeNbt(tag) 
  def isConnected = 
    connectedToFluid || connectedToEnergy || connectedToItem
  override def getRenderAttachmentData(): Object | Null = 
    TunnelRenderAttachmentData(tunnelType, isConnected, outgoing) 
  override def toInitialChunkDataNbt() = 
    createNbt() 
  def tunnelType = tunnelTypeVar
  def setTunnelType(tunnelType: TunnelType) = 
    this.tunnelTypeVar = Some(tunnelType)
    markDirty() 

  def connectedToFluid = connectedToFluidVar 
  def setConnectedToFluid(connected:Boolean) = 
      connectedToFluidVar = connected 
      (this.room, this.tunnel) match 
          case (Some(room), Some(tunnel)) => 
            CompactMachines.roomManager.updateTunnel(room.number, tunnel.copy(connectedToFluid = connected))
            markDirty()
  def connectedToItem = connectedToItemVar
  def setConnectedToItem(connected:Boolean) = 
      connectedToItemVar = connected
      (this.room, this.tunnel) match 
        case (Some(room), Some(tunnel)) =>
          CompactMachines.roomManager.updateTunnel(room.number, tunnel.copy(connectedToItem = connected))
          markDirty() 
  def connectedToEnergy = connectedToEnergyVar
  def setConnectedToEnergy(connected:Boolean) = 
      connectedToEnergyVar = connected 
      (this.room, this.tunnel) match 
        case (Some(room), Some(tunnel)) =>
          CompactMachines.roomManager.updateTunnel(room.number, tunnel.copy(connectedToEnergy = connected))
          markDirty() 
  def outgoing = outgoingVar
  def setOutgoing(outgoing: Boolean): Unit = 
    this.outgoingVar = outgoing 
    (this.room, this.tunnel) match 
      case (Some(room), Some(tunnel)) =>
        CompactMachines.roomManager.updateTunnel(room.number, tunnel.copy(outgoing = outgoing)) 
        markDirty() 
        // redraw this, nerd
        this.world.updateListeners(this.getPos(), this.getCachedState(), this.getCachedState(), Block.NOTIFY_ALL | Block.FORCE_STATE)
  def room : Option[Room] = 
    CompactMachines.roomManager.getRoomByNumber(parentID.getOrElse(-1))
  def tunnel : Option[Tunnel] = 
    this.room.map(TunnelUtil.fromRoomAndPos(_, this.getPos())).flatten
  private def externalHelper[T](lookup: (World, BlockPos, Direction) =>  T): Option[T] = 
    tunnelType match 
      case Some(TunnelType.Normal) =>
        this.room match 
          case Some(room) => 
            val machinePos = room.machine 
            try {
              val machineWorld = getWorld().getServer().getWorld(RegistryKey.of(Registry.WORLD_KEY, room.world)) 
              (this.tunnel.map(_.face.toDirection()).flatten : Option[Direction]) match 
                case Some(dir) => 
                  val targetPos = machinePos.offset(dir, 1) 
                  machineWorld.getChunkManager().addTicket(ChunkTicketType.PORTAL, ChunkPos(targetPos), 3, targetPos)
                  Option(lookup(machineWorld, targetPos, dir)) 
                case None => None 
            } catch {
              case ignored: Exception =>
                CompactMachines.LOGGER.error("Suppressed exception while trying to get external target") 
                CompactMachines.LOGGER.error(ignored) 
                None
            }
          case None => None
      case _ => None
  private def internalHelper[T](lookup: (World, BlockPos, Direction) => T): Option[T] = 
    tunnelType match 
      case Some(TunnelType.Normal) =>
        try { 
          for dir <- Direction.values do
              val newPos = pos.offset(dir, 1) 
              world.getBlockEntity(newPos) match 
                case _ : TunnelWallBlockEntity => None 
                case be => 
                  val storage = lookup(world, newPos, dir) 
                  if storage != null then return Some(storage)
          None
        } catch {
          case ignored: Exception => 
            CompactMachines.LOGGER.error("Surpressed exception while trying to get internal target") 
            CompactMachines.LOGGER.error(ignored) 
            None
        }
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
  def getMachineEntity() : Option[MachineBlockEntity] = 
    try {
      this.room match 
        case Some(room) =>
          val machineWorld = this.getWorld().nn.getServer().nn.getWorld(RegistryKey.of(Registry.WORLD_KEY, room.world)).nn 
          machineWorld.getChunkManager().nn.addTicket(ChunkTicketType.PORTAL, ChunkPos(room.machine), 3, room.machine)
          Option(machineWorld.getBlockEntity(room.machine).asInstanceOf[MachineBlockEntity]) 
        case None => 
          None
    } catch {
      case e: Exception => 
        CompactMachines.LOGGER.error("Surpressed exception while fetching machine entity") 
        CompactMachines.LOGGER.error(e)
        None
    }
object TunnelWallBlockEntity: 
  def tick(world: World, blockPos: BlockPos, blockState: BlockState, tunnelBlockEntity: TunnelWallBlockEntity): Unit = ()
case class TunnelRenderAttachmentData(tunnelType: Option[TunnelType], isConnected: Boolean, isOutgoing: Boolean)
