package hexacraft.game

import hexacraft.util.TickableTimer
import hexacraft.world.*
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.ChunkColumnData
import hexacraft.world.coord.*
import hexacraft.world.entity.*

import com.martomate.nbt.Nbt
import org.joml.Vector2f
import org.zeromq.{SocketType, ZContext, ZMQ, ZMQException}
import zmq.ZError

import scala.collection.mutable

object GameServer {
  def create(isOnline: Boolean, worldProvider: WorldProvider): GameServer = {
    val world = new ServerWorld(worldProvider, worldProvider.getWorldInfo)

    val server = new GameServer(isOnline, worldProvider, world)(using world.size)

    Thread(() => server.run()).start()

    server
  }
}

class GameServer(isOnline: Boolean, worldProvider: WorldProvider, world: ServerWorld)(using CylinderSize) {
  private var serverThread: Thread = null.asInstanceOf[Thread]

  private val rightMouseButtonTimer: TickableTimer = TickableTimer(10, initEnabled = false)
  private val leftMouseButtonTimer: TickableTimer = TickableTimer(10, initEnabled = false)

  private val players: mutable.LongMap[(Player, Entity, Camera)] = mutable.LongMap.empty

  private val collisionDetector: CollisionDetector = new CollisionDetector(world)
  private val playerInputHandler: PlayerInputHandler = new PlayerInputHandler
  private val playerPhysicsHandler: PlayerPhysicsHandler = new PlayerPhysicsHandler(collisionDetector)

  private def saveWorldInfo(): Unit = {
    val worldTag = world.worldInfo.copy(player = players.values.head._1.toNBT).toNBT
    worldProvider.saveWorldData(worldTag)
  }

  def playerEffectiveViscosity(player: Player): Double = {
    player.bounds
      .cover(CylCoords(player.position))
      .map(c => c -> world.getBlock(c))
      .filter(!_._2.blockType.isSolid)
      .map((c, b) =>
        HexBox.approximateVolumeOfIntersection(
          BlockCoords(c).toCylCoords,
          b.blockType.bounds(b.metadata),
          CylCoords(player.position),
          player.bounds
        ) * b.blockType.viscosity.toSI
      )
      .sum
  }

  private def playerVolumeSubmergedInWater(player: Player): Double = {
    val solidBounds = player.bounds.scaledRadially(0.7)
    solidBounds
      .cover(CylCoords(player.position))
      .map(c => c -> world.getBlock(c))
      .filter((c, b) => b.blockType == Block.Water)
      .map((c, b) =>
        HexBox.approximateVolumeOfIntersection(
          BlockCoords(c).toCylCoords,
          b.blockType.bounds(b.metadata),
          CylCoords(player.position),
          solidBounds
        )
      )
      .sum
  }

  def tick(): Unit = {
    try {
      for (playerId, (player, playerEntity, playerCamera)) <- players do {
        val playerCoords = CoordUtils.approximateIntCoords(CylCoords(player.position).toBlockCoords)

        if world.getChunk(playerCoords.getChunkRelWorld).isDefined then {
          val maxSpeed = playerInputHandler.determineMaxSpeed(player.pressedKeys)
          val isInFluid = playerEffectiveViscosity(player) > Block.Air.viscosity.toSI * 2

          playerInputHandler.tick(
            player,
            player.pressedKeys,
            player.mouseMovement,
            maxSpeed,
            isInFluid
          )

          playerPhysicsHandler.tick(
            player,
            maxSpeed,
            playerEffectiveViscosity(player),
            playerVolumeSubmergedInWater(player)
          )
        }

        playerCamera.setPositionAndRotation(player.position, player.rotation)
        playerCamera.updateCoords()
        playerCamera.updateViewMatrix(playerCamera.view.position)

        if rightMouseButtonTimer.tick() then {
          performRightMouseClick(player, playerCamera)
        }
        if leftMouseButtonTimer.tick() then {
          performLeftMouseClick(player, playerCamera)
        }

        playerEntity.transform.position = CylCoords(player.position)
          .offset(0, player.bounds.bottom.toDouble, 0)
        playerEntity.transform.rotation.set(0, math.Pi * 0.5 - player.rotation.y, 0)
        playerEntity.velocity.velocity.set(player.velocity)

        playerEntity.model.foreach(_.tick(playerEntity.velocity.velocity.lengthSquared() > 0.1))
      }

      world.tick(players.values.map(_._3).toSeq)
    } catch {
      case e: ZMQException => println(e)
      case e               => throw e
    }
  }

  def performLeftMouseClick(player: Player, playerCamera: Camera): Unit = {
    val blockAndSide =
      val otherCamera = Camera(playerCamera.proj)
      otherCamera.setPositionAndRotation(player.position, player.rotation)
      otherCamera.updateCoords()
      otherCamera.updateViewMatrix(playerCamera.view.position)
      for
        ray <- Ray.fromScreen(otherCamera, Vector2f(0, 0))
        hit <- new RayTracer(otherCamera, 7).trace(ray, c => Some(world.getBlock(c)).filter(_.blockType.isSolid))
      yield (world.getBlock(hit._1), hit._1, hit._2)

    blockAndSide match {
      case Some((state, coords, _)) =>
        if state.blockType != Block.Air then {
          world.removeBlock(coords)
        }
      case _ =>
    }
  }

  def performRightMouseClick(player: Player, playerCamera: Camera): Unit = {
    val blockAndSide =
      val otherCamera = Camera(playerCamera.proj)
      otherCamera.setPositionAndRotation(player.position, player.rotation)
      otherCamera.updateCoords()
      otherCamera.updateViewMatrix(playerCamera.view.position)
      for
        ray <- Ray.fromScreen(otherCamera, Vector2f(0, 0))
        hit <- new RayTracer(otherCamera, 7).trace(ray, c => Some(world.getBlock(c)).filter(_.blockType.isSolid))
      yield (world.getBlock(hit._1), hit._1, hit._2)

    blockAndSide match {
      case Some((state, coords, Some(side))) =>
        val coordsInFront = coords.offset(NeighborOffsets(side))

        state.blockType match {
          case Block.Tnt => explode(coords)
          case _         => tryPlacingBlockAt(coordsInFront, player, playerCamera)
        }
      case _ =>
    }
  }

  private def tryPlacingBlockAt(coords: BlockRelWorld, player: Player, playerCamera: Camera): Unit = {
    if world.getBlock(coords).blockType.isSolid then {
      return
    }

    val blockType = player.blockInHand
    val state = new BlockState(blockType)

    val collides = world.collisionDetector.collides(
      blockType.bounds(state.metadata),
      BlockCoords(coords).toCylCoords,
      player.bounds,
      CylCoords(playerCamera.position)
    )

    if !collides then {
      world.setBlock(coords, state)
    }
  }

  private def explode(coords: BlockRelWorld): Unit = {
    for dy <- -1 to 1 do {
      for offset <- NeighborOffsets.all do {
        val c = coords.offset(offset).offset(0, dy, 0)
        world.setBlock(c, BlockState.Air)
      }
    }

    world.setBlock(coords, BlockState.Air)
  }

  def unload(): Unit = {
    stop()

    saveWorldInfo()

    world.unload()
  }

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
          val clientId = String(identity).toLong
          val bytes = serverSocket.recv(0)
          if bytes == null then {
            throw new ZMQException(serverSocket.errno())
          }

          val packet = NetworkPacket.deserialize(bytes)
          handlePacket(clientId, packet, serverSocket) match {
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

  private def handlePacket(clientId: Long, packet: NetworkPacket, socket: ZMQ.Socket): Option[Nbt.MapTag] = {
    import NetworkPacket.*

    if !players.contains(clientId) then {
      val player = makePlayer(world)
      val playerEntity = Entity(
        null,
        Seq(
          TransformComponent(CylCoords(player.position).offset(-2, -2, -1)),
          VelocityComponent(),
          BoundsComponent(EntityFactory.playerBounds),
          ModelComponent(PlayerEntityModel.create("player"))
        )
      )
      players(clientId) = (player, playerEntity, new Camera(CameraProjection(70f, 16f / 9f, 0.02f, 100000f)))
    }
    val (player, _, playerCamera) = players(clientId)

    packet match {
      case GetWorldInfo =>
        val info = worldProvider.getWorldInfo
        Some(info.toNBT)
      case LoadChunkData(coords) =>
        world.getChunk(coords) match {
          case Some(chunk) => Some(chunk.toNbt)
          case None        => Some(Nbt.emptyMap) // TODO: return None
        }
      case LoadColumnData(coords) =>
        world.getColumn(coords) match {
          case Some(column) => Some(ChunkColumnData(Some(column.terrainHeight)).toNBT)
          case None         => Some(Nbt.emptyMap) // TODO: return None
        }
      case LoadWorldData =>
        Some(world.worldInfo.toNBT)
      case GetPlayerState =>
        Some(player.toNBT)
      case PlayerRightClicked =>
        performRightMouseClick(player, playerCamera)
        None
      case PlayerLeftClicked =>
        performLeftMouseClick(player, playerCamera)
        None
      case PlayerToggledFlying =>
        player.flying = !player.flying
        None
      case PlayerMovedMouse(dist) =>
        player.mouseMovement = dist
        None
      case PlayerPressedKeys(keys) =>
        player.pressedKeys = keys
        None
    }
  }

  private def makePlayer(world: ServerWorld): Player = {
    given CylinderSize = world.size

    val startX = (math.random() * 100 - 50).toInt
    val startZ = (math.random() * 100 - 50).toInt
    val startY = world.getHeight(startX, startZ) + 4
    Player.atStartPos(BlockCoords(startX, startY, startZ).toCylCoords)
  }

  private def stop(): Unit = {
    if serverThread != null then {
      serverThread.interrupt()
    }
  }
}
