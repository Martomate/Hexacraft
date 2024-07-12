package hexacraft.server

import hexacraft.game.{GameKeyboard, NetworkPacket, PlayerInputHandler, PlayerPhysicsHandler}
import hexacraft.util.Result
import hexacraft.world.*
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.ChunkColumnData
import hexacraft.world.coord.*
import hexacraft.world.entity.*

import com.martomate.nbt.Nbt
import org.joml.Vector2f
import org.zeromq.{SocketType, ZContext, ZMQ, ZMQException}
import zmq.ZError

import java.util.UUID
import scala.collection.mutable

object GameServer {
  def create(isOnline: Boolean, port: Int, worldProvider: WorldProvider): GameServer = {
    val world = new ServerWorld(worldProvider, worldProvider.getWorldInfo)

    val server = new GameServer(isOnline, port, worldProvider, world)(using world.size)

    Thread(() => server.run()).start()

    server
  }
}

case class PlayerData(player: Player, entity: Entity, camera: Camera) {
  var pressedKeys: Seq[GameKeyboard.Key] = Seq.empty
  var mouseMovement: Vector2f = new Vector2f
  val blockUpdatesWaitingToBeSent: mutable.ArrayBuffer[(BlockRelWorld, BlockState)] = mutable.ArrayBuffer.empty
  val entityEventsWaitingToBeSent: mutable.ArrayBuffer[(UUID, EntityEvent)] = mutable.ArrayBuffer.empty
}

class GameServer(isOnline: Boolean, port: Int, worldProvider: WorldProvider, world: ServerWorld)(using CylinderSize) {
  private var serverThread: Thread = null.asInstanceOf[Thread]

  private val players: mutable.LongMap[PlayerData] = mutable.LongMap.empty

  private val collisionDetector: CollisionDetector = new CollisionDetector(world)
  private val playerInputHandler: PlayerInputHandler = new PlayerInputHandler
  private val playerPhysicsHandler: PlayerPhysicsHandler = new PlayerPhysicsHandler(collisionDetector)

  private def saveWorldInfo(): Unit = {
    val worldTag = world.worldInfo.copy(player = players.values.head.player.toNBT).toNBT
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
      for (playerId, p) <- players do {
        val PlayerData(player, entity, camera) = p
        val playerCoords = CoordUtils.approximateIntCoords(CylCoords(player.position).toBlockCoords)

        if world.getChunk(playerCoords.getChunkRelWorld).isDefined then {
          val maxSpeed = playerInputHandler.determineMaxSpeed(p.pressedKeys)
          val isInFluid = playerEffectiveViscosity(player) > Block.Air.viscosity.toSI * 2

          playerInputHandler.tick(
            player,
            p.pressedKeys,
            p.mouseMovement,
            maxSpeed,
            isInFluid
          )
          p.mouseMovement.set(0)

          playerPhysicsHandler.tick(
            player,
            maxSpeed,
            playerEffectiveViscosity(player),
            playerVolumeSubmergedInWater(player)
          )
        }

        camera.setPositionAndRotation(player.position, player.rotation)
        camera.updateCoords()
        camera.updateViewMatrix(camera.view.position)

        entity.transform.position = CylCoords(player.position)
          .offset(0, player.bounds.bottom.toDouble, 0)
        entity.transform.rotation.set(0, math.Pi * 0.5 - player.rotation.y, 0)
        entity.motion.velocity.set(player.velocity)
        entity.motion.flying = player.flying
      }

      val tickResult = world.tick(players.values.map(_.camera).toSeq)

      for coords <- tickResult.blocksUpdated do {
        val blockState = world.getBlock(coords)
        for p <- players.values do {
          p.blockUpdatesWaitingToBeSent.synchronized {
            p.blockUpdatesWaitingToBeSent += coords -> blockState
          }
        }
      }

      for p <- players.values do {
        p.entityEventsWaitingToBeSent.synchronized {
          p.entityEventsWaitingToBeSent ++= tickResult.entityEvents

          for p2 <- players.values do {
            if p != p2 then {
              p.entityEventsWaitingToBeSent += p2.entity.id -> EntityEvent.Position(p2.entity.transform.position)
              p.entityEventsWaitingToBeSent += p2.entity.id -> EntityEvent.Rotation(p2.entity.transform.rotation)
              p.entityEventsWaitingToBeSent += p2.entity.id -> EntityEvent.Velocity(p2.entity.motion.velocity)
              p.entityEventsWaitingToBeSent += p2.entity.id -> EntityEvent.Flying(p2.entity.motion.flying)
            }
          }
        }
      }
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
          notifyPlayersAboutBlockUpdate(coords, BlockState.Air)
        }
      case _ =>
    }
  }

  private def notifyPlayersAboutBlockUpdate(coords: BlockRelWorld, blockState: BlockState): Unit = {
    for (_, playerData) <- players do {
      playerData.blockUpdatesWaitingToBeSent.synchronized {
        playerData.blockUpdatesWaitingToBeSent += coords -> blockState
      }
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
      notifyPlayersAboutBlockUpdate(coords, state)
    }
  }

  private def explode(coords: BlockRelWorld): Unit = {
    for dy <- -1 to 1 do {
      for offset <- NeighborOffsets.all do {
        val c = coords.offset(offset).offset(0, dy, 0)
        world.setBlock(c, BlockState.Air)
        notifyPlayersAboutBlockUpdate(c, BlockState.Air)
      }
    }

    world.setBlock(coords, BlockState.Air)
    notifyPlayersAboutBlockUpdate(coords, BlockState.Air)
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

        if !serverSocket.bind(s"tcp://*:$port") then {
          throw new IllegalStateException("Server could not be bound")
        }
        println(s"Running server on port $port")

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

    println(s"Stopped server")
  }

  private def handlePacket(clientId: Long, packet: NetworkPacket, socket: ZMQ.Socket): Option[Nbt.MapTag] = {
    import NetworkPacket.*

    if !players.contains(clientId) then {
      if !isOnline && players.nonEmpty then {
        return None // only one player may join an offline world
      }

      val player = if !isOnline then {
        val playerNbt = worldProvider.getWorldInfo.player
        if playerNbt != null then {
          Player.fromNBT(playerNbt)
        } else {
          makePlayer(world)
        }
      } else {
        if players.isEmpty then {
          // TODO: temporary solution
          val playerNbt = worldProvider.getWorldInfo.player
          if playerNbt != null then {
            Player.fromNBT(playerNbt)
          } else {
            makePlayer(world)
          }
        } else {
          // TODO: load the player info from disk somehow
          makePlayer(world)
        }
      }
      val entity = Entity(
        Entity.getNextId,
        "player",
        Seq(
          TransformComponent(CylCoords(player.position).offset(-2, -2, -1)),
          MotionComponent(),
          BoundsComponent(EntityFactory.playerBounds)
        )
      )
      val camera = new Camera(CameraProjection(70f, 16f / 9f, 0.02f, 100000f))
      val playerData = PlayerData(player, entity, camera)
      players(clientId) = playerData

      for otherPlayer <- players do {
        val (playerId, otherData) = otherPlayer
        if playerId != clientId then {
          otherData.entityEventsWaitingToBeSent.synchronized {
            otherData.entityEventsWaitingToBeSent += entity.id -> EntityEvent.Spawned(entity.toNBT)
          }
          playerData.entityEventsWaitingToBeSent.synchronized {
            playerData.entityEventsWaitingToBeSent += otherData.entity.id -> EntityEvent.Spawned(otherData.entity.toNBT)
          }
        }
      }
    }
    val playerData = players(clientId)
    val PlayerData(player, _, playerCamera) = playerData

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
      case GetEvents =>
        val updates = playerData.blockUpdatesWaitingToBeSent.synchronized {
          val updates = playerData.blockUpdatesWaitingToBeSent.toSeq
          playerData.blockUpdatesWaitingToBeSent.clear()
          updates
        }

        val updatesNbt =
          for (coords, bs) <- updates
          yield Nbt.makeMap(
            "coords" -> Nbt.LongTag(coords.value),
            "id" -> Nbt.ByteTag(bs.blockType.id),
            "meta" -> Nbt.ByteTag(bs.metadata)
          )

        val entityEvents = playerData.entityEventsWaitingToBeSent.synchronized {
          val entityEvents = playerData.entityEventsWaitingToBeSent.toSeq
          playerData.entityEventsWaitingToBeSent.clear()
          entityEvents
        }

        val ids = entityEvents.map((id, _) => Nbt.StringTag(id.toString))

        val events = for (_, e) <- entityEvents yield {
          val name = e match {
            case EntityEvent.Spawned(_)  => "spawned"
            case EntityEvent.Despawned   => "despawned"
            case EntityEvent.Position(_) => "position"
            case EntityEvent.Rotation(_) => "rotation"
            case EntityEvent.Velocity(_) => "velocity"
            case EntityEvent.Flying(_)   => "flying"
          }

          val extraFields: Seq[(String, Nbt)] = e match {
            case EntityEvent.Spawned(data) => Seq("data" -> data)
            case EntityEvent.Despawned     => Seq()
            case EntityEvent.Position(pos) => Seq("pos" -> Nbt.makeVectorTag(pos.toVector3d))
            case EntityEvent.Rotation(r)   => Seq("r" -> Nbt.makeVectorTag(r))
            case EntityEvent.Velocity(v)   => Seq("v" -> Nbt.makeVectorTag(v))
            case EntityEvent.Flying(f)     => Seq("f" -> Nbt.ByteTag(f))
          }

          Nbt.makeMap(extraFields*).withField("type", Nbt.StringTag(name))
        }

        val response = Nbt.emptyMap
          .withField("block_updates", Nbt.ListTag(updatesNbt))
          .withField("entity_events", Nbt.makeMap("ids" -> Nbt.ListTag(ids), "events" -> Nbt.ListTag(events)))

        Some(response)
      case PlayerRightClicked =>
        performRightMouseClick(player, playerCamera)
        None
      case PlayerLeftClicked =>
        performLeftMouseClick(player, playerCamera)
        None
      case PlayerToggledFlying =>
        player.flying = !player.flying
        None
      case PlayerSetSelectedItemSlot(slot) =>
        player.selectedItemSlot = slot
        None
      case PlayerUpdatedInventory(inv) =>
        player.inventory = inv
        Some(inv.toNBT)
      case PlayerMovedMouse(dist) =>
        playerData.mouseMovement.add(dist)
        None
      case PlayerPressedKeys(keys) =>
        playerData.pressedKeys = keys
        None
      case RunCommand(command, args) =>
        command match {
          case "spawn" =>
            if args.length != 4 then {
              println(s"Wrong number of arguments to spawn command: ${args.length}")
            }
            val entityType = args.head
            val pos = CylCoords(args(1).toDouble, args(2).toDouble, args(3).toDouble)

            EntityFactory.atStartPos(Entity.getNextId, pos, entityType) match {
              case Result.Ok(entity) =>
                world.addEntity(entity)
                println(s"Spawned entity of type $entityType at $pos")
              case Result.Err(e) =>
                println(s"Failed to spawn entity: $e")
            }
          case "kill" =>
            world.removeAllEntities()
            println(s"Removed all entities")
          case _ =>
            println(s"Received unknown command: $command")
        }
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
