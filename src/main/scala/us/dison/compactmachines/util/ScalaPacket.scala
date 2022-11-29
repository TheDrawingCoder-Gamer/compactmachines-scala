package us.dison.compactmachines.util

import net.fabricmc.api.{EnvType, Environment}
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.{PacketByteBufs, PacketSender, ServerPlayNetworking}
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.{ServerPlayNetworkHandler, ServerPlayerEntity}
import net.minecraft.util.Identifier

trait ScalaPacket[T] {
  def id : Identifier
  def read(buf : PacketByteBuf) : T
  def write(buf : PacketByteBuf, input : T) : Unit

  final def asBuf(input: T): PacketByteBuf = {
    val buf = PacketByteBufs.empty()
    write(buf, input)
    buf
  }
  def handleClient(client : MinecraftClient, handler : ClientPlayNetworkHandler, input : T,
                   responseSender : PacketSender) : Unit
  def handleServer(server : MinecraftServer, player: ServerPlayerEntity,
                   handler : ServerPlayNetworkHandler,  input : T, responseSender : PacketSender) : Unit
  final def send(player: ServerPlayerEntity, input: T): Unit = {
    ServerPlayNetworking.send(player, id, asBuf(input))
  }
}

@Environment(EnvType.CLIENT)
object ClientPacket {
  import client.given
  import language.implicitConversions
  extension[T] (packet : ScalaPacket[T]) {
    def registerClient() : Unit = {
      ClientPlayNetworking.registerGlobalReceiver(packet.id, packet)
    }
  }
}

object ServerPacket {
  import server.given
  import language.implicitConversions
  extension[T] (packet : ScalaPacket[T]) {
    def registerServer() : Unit = {
      ServerPlayNetworking.registerGlobalReceiver(packet.id, packet)
    }
  }
}

package client {
  @Environment(EnvType.CLIENT)
  given packetClientHandler[T] : Conversion[ScalaPacket[T], ClientPlayNetworking.PlayChannelHandler] with {
    def apply(packet : ScalaPacket[T]) : ClientPlayNetworking.PlayChannelHandler = {
      (client, handler, buf, responseSender) => {
        val input = packet.read(buf)
        packet.handleClient(client, handler, input, responseSender)
      }
    }
  }
}
package server {
  given packetServerHandler[T] : Conversion[ScalaPacket[T], ServerPlayNetworking.PlayChannelHandler] with {
    override def apply(packet: ScalaPacket[T]): ServerPlayNetworking.PlayChannelHandler = {
      (server, player, handler, buf, responseSender) => {
        val input = packet.read(buf)
        packet.handleServer(server, player, handler, input, responseSender)
      }
    }
  }
}