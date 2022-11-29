package us.dison.compactmachines.network

import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.{ServerPlayNetworkHandler, ServerPlayerEntity}
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import us.dison.compactmachines.CompactMachines
import us.dison.compactmachines.util.ScalaPacket

case class ClientFieldUnwatchPacket(center : BlockPos)

object ClientFieldUnwatchPacket extends ScalaPacket[ClientFieldUnwatchPacket] {
  override val id : Identifier = Identifier(CompactMachines.MOD_ID, "field_unwatch")
  def read(buf : PacketByteBuf) : ClientFieldUnwatchPacket = {
    ClientFieldUnwatchPacket(buf.readBlockPos())
  }
  def write(buf:  PacketByteBuf, input : ClientFieldUnwatchPacket): Unit = {
    buf.writeBlockPos(input.center)
  }

  override def handleClient(client: MinecraftClient, handler: ClientPlayNetworkHandler, input: ClientFieldUnwatchPacket, responseSender: PacketSender): Unit = {

  }

  override def handleServer(server: MinecraftServer, player: ServerPlayerEntity, handler: ServerPlayNetworkHandler, input: ClientFieldUnwatchPacket, responseSender: PacketSender): Unit = ()
}
