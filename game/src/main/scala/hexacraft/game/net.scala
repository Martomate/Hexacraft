package hexacraft.game

import hexacraft.world.{WorldInfo, WorldProvider, WorldSettings}

import com.martomate.nbt.Nbt
import org.zeromq.{SocketType, ZContext, ZMonitor, ZMQ, ZMQException}
import zmq.ZError

import java.nio.charset.Charset
import scala.util.Random

enum NetworkPacket {
  case GetWorldInfo
  case GetState(path: String)
  case PlayerRightClicked
  case PlayerLeftClicked
}

object NetworkPacket {
  def deserialize(bytes: Array[Byte], charset: Charset): NetworkPacket = {
    val message = String(bytes, charset)
    if message == "get_world_info" then {
      NetworkPacket.GetWorldInfo
    } else if message.startsWith("get_state ") then {
      val path = message.substring(10)
      NetworkPacket.GetState(path)
    } else if message == "right_mouse_clicked" then {
      PlayerRightClicked
    } else if message == "left_mouse_clicked" then {
      PlayerLeftClicked
    } else {
      val bytesHex = bytes.map(b => Integer.toHexString(b & 0xff)).mkString("Array(", ", ", ")")
      throw new IllegalArgumentException(s"unknown packet type (message: '$message', raw: $bytesHex)")
    }
  }

  extension (p: NetworkPacket) {
    def serialize(charset: Charset): Array[Byte] =
      val str = p match {
        case NetworkPacket.GetWorldInfo       => "get_world_info"
        case NetworkPacket.GetState(path)     => s"get_state $path"
        case NetworkPacket.PlayerRightClicked => "right_mouse_clicked"
        case NetworkPacket.PlayerLeftClicked  => "left_mouse_clicked"
      }
      str.getBytes(charset)
  }
}

class RemoteWorldProvider(client: GameClient) extends WorldProvider {
  override def getWorldInfo: WorldInfo = {
    val tag = client.query(NetworkPacket.GetWorldInfo)
    WorldInfo.fromNBT(tag.asInstanceOf[Nbt.MapTag], null, WorldSettings.none)
  }

  override def loadState(path: String): Option[Nbt.MapTag] = {
    val tag = client.query(NetworkPacket.GetState(path))
    Some(tag.asInstanceOf[Nbt.MapTag])
  }

  override def saveState(tag: Nbt.MapTag, name: String, path: String): Unit = {
    // throw new UnsupportedOperationException()
    ()
  }
}

class GameClient(serverIp: String, serverPort: Int) {
  private val clientId = (new Random().nextInt(1000000) + 1000000).toString.substring(1)

  private val context = ZContext()
  context.setUncaughtExceptionHandler((thread, exc) => println(s"Uncaught exception: $exc"))
  context.setNotificationExceptionHandler((thread, exc) => println(s"Notification: $exc"))

  private val socket = context.createSocket(SocketType.DEALER)

  private var _shouldLogout = false
  def shouldLogout: Boolean = _shouldLogout

  private var monitoringThread: Thread = _

  def runMonitoring(): Unit = {
    if monitoringThread != null then {
      throw new Exception("May only run monitoring once")
    }
    monitoringThread = Thread.currentThread()

    val monitor = ZMonitor(context, socket)
    monitor.add(ZMonitor.Event.ALL)
    monitor.verbose(true)
    monitor.start()
    try {
      while !Thread.interrupted() do {
        val event = monitor.nextEvent(100)
        if event != null then {
          println(event)

          if event.`type` == ZMonitor.Event.DISCONNECTED then {
            _shouldLogout = true
          }
        }
      }
    } catch {
      case e: ZMQException =>
        e.getErrorCode match {
          case ZError.EINTR => // noop
          case _            => throw e
        }
      case e => throw e
    }
    monitor.close()
  }

  socket.setIdentity(clientId.getBytes)
  socket.setSendTimeOut(3000)
  socket.setReceiveTimeOut(3000)
  socket.setReconnectIVL(-1)
  socket.setHeartbeatIvl(200)
  socket.setHeartbeatTimeout(1000)
  socket.connect(s"tcp://$serverIp:$serverPort")

  def notify(packet: NetworkPacket): Unit = this.synchronized {
    val message = packet.serialize(ZMQ.CHARSET)

    if !socket.send(message) then {
      val err = socket.errno()
      ZMQ.EVENT_ALL
      throw new ZMQException("Could not send message", err)
    }
  }

  private def queryRaw(message: Array[Byte]): Array[Byte] = this.synchronized {
    if !socket.send(message) then {
      val err = socket.errno()
      throw new ZMQException("Could not send message", err)
    }

    val response = socket.recv(0)
    if response == null then {
      val err = socket.errno()
      throw new ZMQException("Could not receive message", err)
    }

    response
  }

  def query(packet: NetworkPacket): Nbt = {
    val response = queryRaw(packet.serialize(ZMQ.CHARSET))
    val (_, tag) = Nbt.fromBinary(response)
    tag
  }

  def close(): Unit = {
    context.close()
    if monitoringThread != null then {
      monitoringThread.interrupt()
    }
  }
}

class GameServer(worldProvider: WorldProvider, game: GameScene) {
  private var serverThread: Thread = _

  def run(): Unit = {
    if serverThread != null then {
      throw new RuntimeException("You may only start the server once")
    }
    serverThread = Thread.currentThread()

    try {
      val context = ZContext()
      try {
        val serverSocket = context.createSocket(SocketType.ROUTER)
        serverSocket.setHeartbeatIvl(1000)
        serverSocket.setHeartbeatTtl(3000)
        serverSocket.setHeartbeatTimeout(3000)

        val serverPort = 1234
        if !serverSocket.bind(s"tcp://*:$serverPort") then {
          throw new IllegalStateException("Server could not be bound")
        }
        println(s"Running server on port $serverPort")

        while !Thread.currentThread().isInterrupted do {
          val identity = serverSocket.recv(0)
          if identity.isEmpty then {
            throw new Exception("Received an empty identity frame")
          }
          val bytes = serverSocket.recv(0)
          if bytes == null then {
            throw new ZMQException(serverSocket.errno())
          }

          val packet = NetworkPacket.deserialize(bytes, ZMQ.CHARSET)
          handlePacket(packet, serverSocket) match {
            case Some(res) =>
              serverSocket.sendMore(identity)
              serverSocket.send(res.toBinary())
            case None =>
          }
        }
      } finally context.close()
    } catch {
      case e: ZMQException =>
        e.getErrorCode match {
          case ZError.EINTR => // noop
          case _            => throw e
        }
      case e =>
        throw e
    }

    println(s"Stopping server")
  }

  private def handlePacket(packet: NetworkPacket, socket: ZMQ.Socket): Option[Nbt.MapTag] = {
    import NetworkPacket.*

    packet match {
      case GetWorldInfo =>
        val info = worldProvider.getWorldInfo
        Some(info.toNBT)
      case GetState(path) =>
        Some(worldProvider.loadState(path).getOrElse(Nbt.emptyMap))
      case PlayerRightClicked =>
        println("right click!")
        game.performRightMouseClick()
        None
      case PlayerLeftClicked =>
        println("left click!")
        game.performLeftMouseClick()
        None
    }
  }

  def stop(): Unit = {
    if serverThread != null then {
      serverThread.interrupt()
    }
  }
}

class NetworkHandler(val isHosting: Boolean, isOnline: Boolean, val worldProvider: WorldProvider, client: GameClient) {
  private var server: GameServer = _

  def runServer(game: GameScene): Unit = if isOnline then {
    server = GameServer(worldProvider, game)
    server.run()
  }

  def runClient(): Unit = {
    if client != null then {
      new Thread(() => client.runMonitoring()).start()
    }
  }

  def unload(): Unit = {
    if client != null then {
      client.close()
    }
    if server != null then {
      server.stop()
    }
  }

  def shouldLogout: Boolean = client != null && client.shouldLogout

  def notifyServer(packet: NetworkPacket): Unit = {
    println(s"Sending to server: $packet")
    client.notify(packet)
  }
}
