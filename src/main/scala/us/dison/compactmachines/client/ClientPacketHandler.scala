package us.dison.compactmachines.client

import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.BlockPos

object ClientPacketHandler {
  def handleFieldDeactivate(client : MinecraftClient, center : BlockPos) : Unit= {
    client.execute(() => {

    })
  }
}
