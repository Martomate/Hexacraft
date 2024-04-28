package hexacraft.game

import hexacraft.game.ServerNetworkPacket.{ChunkData, ColumnData, WorldData, WorldInfoPacket}
import hexacraft.world.{WorldInfo, WorldProvider, WorldSettings}
import hexacraft.world.block.Block
import hexacraft.world.coord.{ChunkRelWorld, ColumnRelWorld}

import com.martomate.nbt.Nbt
import org.joml.{Vector2f, Vector3d}
import org.zeromq.{SocketType, ZContext, ZMonitor, ZMQ, ZMQException}
import zmq.ZError

import java.nio.charset.Charset
import scala.collection.mutable
import scala.util.Random

enum NetworkPacket {
  case GetWorldInfo
  case LoadChunkData(coords: ChunkRelWorld)
  case LoadColumnData(coords: ColumnRelWorld)
  case LoadWorldData
  case PlayerRightClicked
  case PlayerLeftClicked
  case PlayerMovedMouse(distance: Vector2f)
  case PlayerPressedKeys(keys: Seq[GameKeyboard.Key])
}

object NetworkPacket {
  def deserialize(bytes: Array[Byte], charset: Charset): NetworkPacket = {
    val message = String(bytes, charset)
    if message == "get_world_info" then {
      NetworkPacket.GetWorldInfo
    } else if message.startsWith("load_chunk_data ") then {
      val coords = ChunkRelWorld(message.substring(16).toLong)
      NetworkPacket.LoadChunkData(coords)
    } else if message.startsWith("load_column_data ") then {
      val coords = ColumnRelWorld(message.substring(17).toLong)
      NetworkPacket.LoadColumnData(coords)
    } else if message.startsWith("load_world_data") then {
      NetworkPacket.LoadWorldData
    } else if message == "right_mouse_clicked" then {
      PlayerRightClicked
    } else if message == "left_mouse_clicked" then {
      PlayerLeftClicked
    } else if message.startsWith("mouse_moved ") then {
      val args = message.substring(12).split(' ')
      val dx = args(0).toFloat
      val dy = args(1).toFloat
      PlayerMovedMouse(new Vector2f(dx, dy))
    } else if message.startsWith("keys_pressed ") then {
      val keys = message.substring(13).split(' ').toSeq.filter(_ != "").map(s => GameKeyboard.Key.valueOf(s))
      PlayerPressedKeys(keys)
    } else {
      val bytesHex = bytes.map(b => Integer.toHexString(b & 0xff)).mkString("Array(", ", ", ")")
      throw new IllegalArgumentException(s"unknown packet type (message: '$message', raw: $bytesHex)")
    }
  }

  extension (p: NetworkPacket) {
    def serialize(charset: Charset): Array[Byte] =
      val str = p match {
        case NetworkPacket.GetWorldInfo            => "get_world_info"
        case NetworkPacket.LoadChunkData(coords)   => s"load_chunk_data ${coords.value}"
        case NetworkPacket.LoadColumnData(coords)  => s"load_column_data ${coords.value}"
        case NetworkPacket.LoadWorldData           => "load_world_data"
        case NetworkPacket.PlayerRightClicked      => "right_mouse_clicked"
        case NetworkPacket.PlayerLeftClicked       => "left_mouse_clicked"
        case NetworkPacket.PlayerMovedMouse(dist)  => s"mouse_moved ${dist.x} ${dist.y}"
        case NetworkPacket.PlayerPressedKeys(keys) => s"keys_pressed ${keys.mkString(" ")}"
      }
      str.getBytes(charset)
  }
}

enum ServerNetworkPacket {
  case WorldInfoPacket(info: WorldInfo)
  case ChunkData(coords: ChunkRelWorld, data: Nbt)
  case ColumnData(coords: ColumnRelWorld, data: Nbt)
  case WorldData(data: Nbt)
  case PlayerPosition(playerId: Long, position: Vector3d)
  case PlayerRotation(playerId: Long, rotation: Vector3d)
}

object ServerNetworkPacket {
  def deserialize(bytes: Array[Byte]): ServerNetworkPacket = {
    val (packetName, packetDataTag) = Nbt.fromBinary(bytes)
    val root = packetDataTag.asInstanceOf[Nbt.MapTag]

    packetName match {
      case "world_info" =>
        val infoTag = root.getMap("info").get
        val info = WorldInfo.fromNBT(infoTag, null, WorldSettings.none)
        ServerNetworkPacket.WorldInfoPacket(info)
      case "chunk_data" =>
        val coords = ChunkRelWorld(root.getLong("coords", -1L))
        val data = root.getMap("data").get
        ServerNetworkPacket.ChunkData(coords, data)
      case "column_data" =>
        val coords = ColumnRelWorld(root.getLong("coords", -1L))
        val data = root.getMap("data").get
        ServerNetworkPacket.ColumnData(coords, data)
      case "world_data" =>
        val data = root.getMap("data").get
        ServerNetworkPacket.WorldData(data)
      case "player_position" =>
        val playerId = root.getLong("playerId", -1L)
        val position = root.getMap("position").get.setVector(new Vector3d)
        ServerNetworkPacket.PlayerPosition(playerId, position)
      case "player_rotation" =>
        val playerId = root.getLong("playerId", -1L)
        val rotation = root.getMap("rotation").get.setVector(new Vector3d)
        ServerNetworkPacket.PlayerRotation(playerId, rotation)
      case _ =>
        throw new IllegalArgumentException(s"unknown server packet type '$packetName'")
    }
  }

  extension (p: ServerNetworkPacket) {
    def serialize(): Array[Byte] =
      p match {
        case WorldInfoPacket(info) =>
          val tag = Nbt.makeMap(
            "info" -> info.toNBT
          )
          tag.toBinary("world_info")
        case ServerNetworkPacket.ChunkData(coords, data) =>
          val tag = Nbt.makeMap(
            "coords" -> Nbt.LongTag(coords.value),
            "data" -> data
          )
          tag.toBinary("chunk_data")
        case ServerNetworkPacket.ColumnData(coords, data) =>
          val tag = Nbt.makeMap(
            "coords" -> Nbt.LongTag(coords.value),
            "data" -> data
          )
          tag.toBinary("column_data")
        case ServerNetworkPacket.WorldData(data) =>
          val tag = Nbt.makeMap(
            "data" -> data
          )
          tag.toBinary("world_data")
        case ServerNetworkPacket.PlayerPosition(playerId, position) =>
          val tag = Nbt.makeMap(
            "playerId" -> Nbt.LongTag(playerId),
            "position" -> Nbt.makeVectorTag(position)
          )
          tag.toBinary("player_position")
        case ServerNetworkPacket.PlayerRotation(playerId, rotation) =>
          val tag = Nbt.makeMap(
            "playerId" -> Nbt.LongTag(playerId),
            "rotation" -> Nbt.makeVectorTag(rotation)
          )
          tag.toBinary("player_rotation")
      }
  }
}

class RemoteWorldProvider(client: GameClient) extends WorldProvider {
  override def getWorldInfo: WorldInfo = {
    client.query(NetworkPacket.GetWorldInfo) match {
      case WorldInfoPacket(info) => info
      case packet                => throw new IllegalStateException(s"Expected WorldInfoPacket but got $packet")
    }
  }

  override def loadChunkData(coords: ChunkRelWorld): Option[Nbt.MapTag] = {
    val tag = client.query(NetworkPacket.LoadChunkData(coords)) match {
      case ChunkData(coords, data) => data
      case packet                  => throw new IllegalStateException(s"Expected ChunkData but got $packet")
    }
    Some(tag.asInstanceOf[Nbt.MapTag])
  }

  override def saveChunkData(tag: Nbt.MapTag, coords: ChunkRelWorld): Unit = {}

  override def loadColumnData(coords: ColumnRelWorld): Option[Nbt.MapTag] = {
    val tag = client.query(NetworkPacket.LoadColumnData(coords)) match {
      case ColumnData(coords, data) => data
      case packet                   => throw new IllegalStateException(s"Expected ColumnData but got $packet")
    }
    Some(tag.asInstanceOf[Nbt.MapTag])
  }

  override def saveColumnData(tag: Nbt.MapTag, coords: ColumnRelWorld): Unit = {}

  override def loadWorldData(): Option[Nbt.MapTag] = {
    val tag = client.query(NetworkPacket.LoadWorldData) match {
      case WorldData(data) => data
      case packet          => throw new IllegalStateException(s"Expected WorldData but got $packet")
    }
    Some(tag.asInstanceOf[Nbt.MapTag])
  }

  override def saveWorldData(tag: Nbt.MapTag): Unit = {}
}

class GameClient(serverIp: String, serverPort: Int) {
  val clientId = new Random().nextInt(1000000) + 1000000

  private val context = ZContext()
  context.setUncaughtExceptionHandler((thread, exc) => println(s"Uncaught exception: $exc"))
  context.setNotificationExceptionHandler((thread, exc) => println(s"Notification: $exc"))

  private val socket = context.createSocket(SocketType.DEALER)

  private var _shouldLogout = false
  def shouldLogout: Boolean = _shouldLogout

  private var monitoringThread: Thread = null.asInstanceOf[Thread]

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

  socket.setIdentity(clientId.toString.getBytes)
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

  def tryReceive(): Option[ServerNetworkPacket] = {
    val message = socket.recv(ZMQ.DONTWAIT)
    if message != null then {
      Some(ServerNetworkPacket.deserialize(message))
    } else {
      None
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

  def query(packet: NetworkPacket): ServerNetworkPacket = {
    val response = queryRaw(packet.serialize(ZMQ.CHARSET))
    ServerNetworkPacket.deserialize(response)
  }

  def close(): Unit = {
    context.close()
    if monitoringThread != null then {
      monitoringThread.interrupt()
    }
  }
}

class ClientData()

class GameServer(worldProvider: WorldProvider, game: GameScene, serverPort: Int) {
  private var serverThread: Thread = null.asInstanceOf[Thread]

  private val clients: mutable.LongMap[ClientData] = mutable.LongMap.empty
  private var serverSocket: ZMQ.Socket = null.asInstanceOf[ZMQ.Socket]

  def run(): Unit = {
    if serverThread != null then {
      throw new RuntimeException("You may only start the server once")
    }
    serverThread = Thread.currentThread()

    try {
      val context = ZContext()
      try {
        serverSocket = context.createSocket(SocketType.ROUTER)
        serverSocket.setHeartbeatIvl(1000)
        serverSocket.setHeartbeatTtl(3000)
        serverSocket.setHeartbeatTimeout(3000)

        if !serverSocket.bind(s"tcp://*:$serverPort") then {
          throw new IllegalStateException("Server could not be bound")
        }
        println(s"Running server on port $serverPort")

        while !Thread.currentThread().isInterrupted do {
          val identity = serverSocket.recv(0)
          if identity.isEmpty then {
            throw new Exception("Received an empty identity frame")
          }
          val clientId = String(identity).toLong
          val clientData = clients.getOrElseUpdate(clientId, new ClientData())

          val bytes = serverSocket.recv(0)
          if bytes == null then {
            throw new ZMQException(serverSocket.errno())
          }

          val packet = NetworkPacket.deserialize(bytes, ZMQ.CHARSET)
          handlePacket(packet, clientId, serverSocket)
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

  private def handlePacket(packet: NetworkPacket, clientId: Long, socket: ZMQ.Socket): Unit = {
    import NetworkPacket.*

    packet match {
      case GetWorldInfo =>
        val info = worldProvider.getWorldInfo

        socket.sendMore(clientId.toString.getBytes)
        socket.send(ServerNetworkPacket.WorldInfoPacket(info).serialize())
      case LoadChunkData(coords) =>
        val data = worldProvider.loadChunkData(coords).getOrElse(Nbt.emptyMap)

        socket.sendMore(clientId.toString.getBytes)
        socket.send(ServerNetworkPacket.ChunkData(coords, data).serialize())
      case LoadColumnData(coords) =>
        val data = worldProvider.loadColumnData(coords).getOrElse(Nbt.emptyMap)

        socket.sendMore(clientId.toString.getBytes)
        socket.send(ServerNetworkPacket.ColumnData(coords, data).serialize())
      case LoadWorldData =>
        val data = worldProvider.loadWorldData().getOrElse(Nbt.emptyMap)

        socket.sendMore(clientId.toString.getBytes)
        socket.send(ServerNetworkPacket.WorldData(data).serialize())
      case PlayerRightClicked =>
        game.performRightMouseClick(false)
      case PlayerLeftClicked =>
        game.performLeftMouseClick(false)
      case PlayerMovedMouse(dist) =>
        val rSpeed = 0.05
        game.otherPlayer.rotation.y += dist.x * rSpeed * 0.05
        game.otherPlayer.rotation.x -= dist.y * rSpeed * 0.05

      // TODO: uncomment the lines below and make sure the client can handle a message before the query response
      /*for otherClientId <- this.clients.keys do {
          if otherClientId != clientId then {
            socket.sendMore(otherClientId.toString.getBytes)
            socket.send(
              ServerNetworkPacket.PlayerRotation(clientId, game.otherPlayer.rotation).serialize()
            )
          }
        }*/
      case PlayerPressedKeys(keys) =>
        val isInFluid = game.playerEffectiveViscosity(game.otherPlayer) > Block.Air.viscosity.toSI * 2

        val playerInputHandler = PlayerInputHandler()
        val maxSpeed = playerInputHandler.determineMaxSpeed(keys)
        playerInputHandler.tick(game.otherPlayer, keys, Vector2f(), maxSpeed, isInFluid)

    }
  }

  def stop(): Unit = {
    if serverThread != null then {
      serverThread.interrupt()
    }
  }
}

object NetworkHandler {
  def forServer(worldProvider: WorldProvider, serverPort: Int): NetworkHandler =
    NetworkHandler(true, true, worldProvider, null, serverPort)

  def forClient(worldProvider: WorldProvider, client: GameClient, serverPort: Int): NetworkHandler =
    NetworkHandler(false, true, worldProvider, client, serverPort)

  def forOffline(worldProvider: WorldProvider): NetworkHandler =
    NetworkHandler(true, false, worldProvider, null, -42)
}

class NetworkHandler(
    val isHosting: Boolean,
    val isOnline: Boolean,
    val worldProvider: WorldProvider,
    val client: GameClient,
    serverPort: Int
) {
  private var server: GameServer = null.asInstanceOf[GameServer]

  def runServer(game: GameScene): Unit = if isOnline then {
    server = GameServer(worldProvider, game, serverPort)
    server.run()
  }

  def runClient(): Unit = {
    if client != null then {
      new Thread(() => client.runMonitoring()).start()
    }
  }

  def tryReceiveMessagesFromServer(): Option[ServerNetworkPacket] = {
    if client != null then {
      client.tryReceive()
    } else {
      None
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
