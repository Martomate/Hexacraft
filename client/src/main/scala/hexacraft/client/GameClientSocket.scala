package hexacraft.client

import hexacraft.game.{NetworkException, NetworkPacket}
import hexacraft.nbt.Nbt
import hexacraft.rs.RustLib

import scala.util.Random

class GameClientSocket(serverIp: String, serverPort: Int) {
  private val clientId = (new Random().nextInt(1000000) + 1000000).toString
  private val socketHandle = RustLib.ClientSocket.create(clientId.getBytes())

  RustLib.ClientSocket.connect(socketHandle, serverIp, serverPort)

  def sendPacket(packet: NetworkPacket): Unit = this.synchronized {
    doSend(packet.serialize())
  }

  def sendPacketAndWait(packet: NetworkPacket): Nbt = this.synchronized {
    doSend(packet.serialize())

    val (_, tag) = Nbt.fromBinary(doReceive())
    tag
  }

  def sendMultiplePacketsAndWait(packets: Seq[NetworkPacket]): Seq[Nbt] = this.synchronized {
    for p <- packets do {
      doSend(p.serialize())
    }

    for i <- packets.indices yield {
      val (_, tag) = Nbt.fromBinary(doReceive())
      tag
    }
  }

  private def doSend(message: Array[Byte]): Unit = {
    try {
      RustLib.ClientSocket.send(socketHandle, message)
    } catch {
      case e: RuntimeException => throw new NetworkException(s"Could not send message: ${e.getMessage}")
    }
  }

  private def doReceive() = {
    try {
      RustLib.ClientSocket.receive(socketHandle)
    } catch {
      case e: RuntimeException => throw new NetworkException(s"Could not receive message: ${e.getMessage}")
    }
  }

  def close(): Unit = {
    RustLib.ClientSocket.close(socketHandle)
  }
}
