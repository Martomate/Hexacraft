package hexacraft.client

import hexacraft.game.NetworkPacket
import hexacraft.nbt.Nbt

class GameClientSocket(channel: NetworkChannel) {
  private val messageTracker = new MessageTracker

  def sendPacket(packet: NetworkPacket): Unit = {
    messageTracker.trackNotification {
      channel.send(packet.serialize())
    }
  }

  def sendPacketAndWait(packet: NetworkPacket): Nbt = {
    messageTracker.trackRequest {
      channel.send(packet.serialize())
    } {
      val (_, tag) = Nbt.fromBinary(channel.receive())
      tag
    }
  }

  def sendMultiplePacketsAndWait(packets: Seq[NetworkPacket]): Seq[Nbt] = {
    messageTracker.trackRequest {
      for p <- packets do {
        channel.send(p.serialize())
      }
    } {
      for i <- packets.indices yield {
        val (_, tag) = Nbt.fromBinary(channel.receive())
        tag
      }
    }
  }

  def close(): Unit = {
    channel.close()
  }
}
