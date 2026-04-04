package hexacraft.client

import hexacraft.game.NetworkException
import hexacraft.rs.RustLib

import scala.util.Random

trait NetworkChannel {
  def send(data: Array[Byte]): Unit
  def tryReceive(): Option[Array[Byte]]
  def close(): Unit
  def isClosed: Boolean

  def receive(): Array[Byte] = {
    val startTime = System.currentTimeMillis
    while !isClosed do {
      this.tryReceive() match {
        case Some(data) =>
          return data
        case None =>
          val timeMs = System.currentTimeMillis - startTime
          if timeMs > 1000 then {
            throw new RuntimeException(s"Timed out receiving data after $timeMs ms")
          }
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
