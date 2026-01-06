package hexacraft.server

import hexacraft.game.{NetworkException, NetworkPacket}
import hexacraft.nbt.Nbt
import hexacraft.rs.RustLib
import hexacraft.util.Result
import hexacraft.util.Result.{Err, Ok}

import java.net.{DatagramSocket, InetAddress}

object TcpServer {
  enum Error {
    case InvalidPacket(message: String)
  }

  def start(port: Int): Result[TcpServer, String] = {
    val socketHandle = RustLib.ServerSocket.create()

    try {
      RustLib.ServerSocket.bind(socketHandle, port)
    } catch {
      case e: RuntimeException =>
        RustLib.ServerSocket.close(socketHandle)
        return Err(s"Server could not be bound: ${e.getMessage}")
    }

    Ok(new TcpServer(socketHandle, port))
  }
}

class TcpServer private (socketHandle: Long, val localPort: Int) {
  private var _running = true

  def running: Boolean = _running

  val localAddress: String = {
    val socket = new DatagramSocket
    try {
      socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
      socket.getLocalAddress.getHostAddress
    } catch {
      case _: Exception => "127.0.0.1"
    } finally {
      socket.close()
    }
  }

  def receive(): Result[(Long, NetworkPacket), TcpServer.Error] = {
    if !_running then {
      throw new IllegalStateException("The server is not running")
    }

    val identity = doReceive()
    val bytes = doReceive()

    if identity.isEmpty then {
      return Err(TcpServer.Error.InvalidPacket("Received an empty identity frame"))
    }
    for {
      clientId <- Result
        .attempt(String(identity).toLong)
        .mapErr(_ => TcpServer.Error.InvalidPacket("Client ID was not an integer"))
      packet <- Result
        .attempt(NetworkPacket.deserialize(bytes))
        .mapErr(e => TcpServer.Error.InvalidPacket(e.getMessage))
    } yield (clientId, packet)
  }

  private def doReceive(): Array[Byte] = {
    try {
      RustLib.ServerSocket.receive(socketHandle)
    } catch {
      case e: RuntimeException =>
        throw new NetworkException(e.getMessage)
    }
  }

  def send(clientId: Long, data: Nbt): Result[Unit, TcpServer.Error] = {
    if !_running then {
      throw new IllegalStateException("The server is not running")
    }

    doSend(clientId, data)
    Ok(())
  }

  private def doSend(clientId: Long, data: Nbt): Unit = {
    try {
      RustLib.ServerSocket.send(socketHandle, clientId.toString.getBytes(), data.toBinary())
    } catch {
      case e: RuntimeException =>
        throw new NetworkException(e.getMessage)
    }
  }

  /** Note: Run this instead of interrupting the thread */
  def stop(): Unit = {
    if !_running then {
      throw new IllegalStateException("The server is not running")
    }
    _running = false
    RustLib.ServerSocket.close(socketHandle)
  }

  def unload(): Unit = {
    if _running then {
      stop()
    }
  }
}
