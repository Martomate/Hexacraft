package hexacraft.client

import hexacraft.game.{NetworkException, NetworkPacket}
import hexacraft.nbt.Nbt
import hexacraft.rs.RustLib

import scala.util.Random

trait NetworkChannel {
  def send(data: Array[Byte]): Unit
  def tryReceive(): Option[Array[Byte]]
  def close(): Unit
  def isClosed: Boolean

  def receive(): Array[Byte] = {
    while !isClosed do {
      this.tryReceive() match {
        case Some(data) =>
          return data
        case None =>
          Thread.sleep(1)
      }
    }
    throw new RuntimeException("Channel is closed")
  }
}

object NetworkChannel {
  def client(serverIp: String, serverPort: Int): NetworkChannel = new NetworkChannel {
    private val clientId = (new Random().nextInt(1000000) + 1000000).toString
    private val socketHandle = RustLib.ClientSocket.create(clientId.getBytes())
    RustLib.ClientSocket.connect(socketHandle, serverIp, serverPort)

    private var _isClosed = false

    override def send(data: Array[Byte]): Unit = {
      try {
        RustLib.ClientSocket.send(socketHandle, data)
      } catch {
        case e: RuntimeException => throw new NetworkException(s"Could not send message: ${e.getMessage}")
      }
    }

    override def tryReceive() = {
      try {
        Option(RustLib.ClientSocket.tryReceive(socketHandle))
      } catch {
        case e: RuntimeException => throw new NetworkException(s"Could not receive message: ${e.getMessage}")
      }
    }

    override def close(): Unit = {
      RustLib.ClientSocket.close(socketHandle)
      _isClosed = true
    }

    override def isClosed: Boolean = _isClosed
  }
}

class GameClientSocket(channel: NetworkChannel) {
  private val sendLock = new Object()
  private val receiveLock = new Object()

  def sendPacket(packet: NetworkPacket): Unit = {
    sendLock.synchronized {
      channel.send(packet.serialize())
    }
  }

  def sendPacketAndWait(packet: NetworkPacket): Nbt = {
    sendLock.synchronized {
      channel.send(packet.serialize())
    }

    receiveLock.synchronized {
      val (_, tag) = Nbt.fromBinary(channel.receive())
      tag
    }
  }

  def sendMultiplePacketsAndWait(packets: Seq[NetworkPacket]): Seq[Nbt] = {
    sendLock.synchronized {
      for p <- packets do {
        channel.send(p.serialize())
      }
    }

    receiveLock.synchronized {
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
