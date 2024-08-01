package hexacraft.server

import hexacraft.game.NetworkPacket
import hexacraft.util.Result
import hexacraft.util.Result.{Err, Ok}

import com.martomate.nbt.Nbt
import org.zeromq.{SocketType, ZContext, ZMQ, ZMQException}
import zmq.ZError

object TcpServer {
  enum Error {
    case InvalidPacket(message: String)
  }

  def start(port: Int): Result[TcpServer, String] = {
    val context = ZContext()
    val serverSocket = context.createSocket(SocketType.ROUTER)

    serverSocket.setHeartbeatIvl(1000)
    serverSocket.setHeartbeatTtl(3000)
    serverSocket.setHeartbeatTimeout(3000)

    if !serverSocket.bind(s"tcp://*:$port") then {
      context.close()
      return Err("Server could not be bound")
    }

    Ok(new TcpServer(context, serverSocket))
  }
}

class TcpServer private (context: ZContext, serverSocket: ZMQ.Socket) {
  private var _running = true

  def running: Boolean = _running

  def receive(): Result[(Long, NetworkPacket), TcpServer.Error] = {
    if !_running then {
      throw new IllegalStateException("The server is not running")
    }

    try {
      val identity = serverSocket.recv(0)
      if identity == null then {
        throw new ZMQException(serverSocket.errno())
      }

      val bytes = serverSocket.recv(0)
      if bytes == null then {
        throw new ZMQException(serverSocket.errno())
      }

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
    } catch {
      case e: ZMQException if e.getErrorCode == ZError.ETERM =>
        throw new InterruptedException()
    }
  }

  def send(clientId: Long, data: Nbt): Result[Unit, TcpServer.Error] = {
    if !_running then {
      throw new IllegalStateException("The server is not running")
    }

    try {
      serverSocket.sendMore(clientId.toString)
      serverSocket.send(data.toBinary())
      Ok(())
    } catch {
      case e: ZMQException if e.getErrorCode == ZError.ETERM =>
        throw new InterruptedException()
    }
  }

  /** Note: Run this instead of interrupting the thread */
  def stop(): Unit = {
    if !_running then {
      throw new IllegalStateException("The server is not running")
    }

    _running = false
    context.close()
  }
}
