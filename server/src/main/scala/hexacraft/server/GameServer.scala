package hexacraft.server

import hexacraft.game.{GameKeyboard, NetworkPacket, PlayerInputHandler, PlayerPhysicsHandler, ServerMessage}
import hexacraft.nbt.Nbt
import hexacraft.server.TcpServer.Error
import hexacraft.server.world.ServerWorld
import hexacraft.util.{Result, SeqUtils}
import hexacraft.world.*
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.ChunkColumnData
import hexacraft.world.coord.*
import hexacraft.world.entity.*

import org.joml.{Vector2f, Vector3d}
import org.zeromq.ZMQException

import java.util.UUID
import scala.collection.mutable

object GameServer {
  def create(isOnline: Boolean, port: Int, worldProvider: WorldProvider): GameServer = {
    val world = new ServerWorld(worldProvider, worldProvider.getWorldInfo)

    val tcpServer = TcpServer
      .start(port)
      .unwrapWith(m => new IllegalStateException(s"Could not start server: $m"))

    new GameServer(isOnline, tcpServer, worldProvider, world)(using world.size)
  }
}

case class PlayerData(player: Player, entity: Entity, camera: Camera) {
  var pressedKeys: Seq[GameKeyboard.Key] = Seq.empty
  var mouseMovement: Vector2f = new Vector2f
  val blockUpdatesWaitingToBeSent: mutable.ArrayBuffer[(BlockRelWorld, BlockState)] = mutable.ArrayBuffer.empty
  val entityEventsWaitingToBeSent: mutable.ArrayBuffer[(UUID, EntityEvent)] = mutable.ArrayBuffer.empty
  val messagesWaitingToBeSent: mutable.ArrayBuffer[ServerMessage] = mutable.ArrayBuffer.empty

  var lastSeen: Long = System.currentTimeMillis
  var shouldBeKicked: Boolean = false
}

class GameServer(
    isOnline: Boolean,
    server: TcpServer,
    worldProvider: WorldProvider,
    world: ServerWorld
)(using CylinderSize) {

  private var isShuttingDown: Boolean = false

  private val players: mutable.LongMap[PlayerData] = mutable.LongMap.empty

  private val collisionDetector: CollisionDetector = new CollisionDetector(world)
  private val playerInputHandler: PlayerInputHandler = new PlayerInputHandler
  private val playerPhysicsHandler: PlayerPhysicsHandler = new PlayerPhysicsHandler(collisionDetector)

  private val serverThread: Thread = Thread(() => this.run())
  serverThread.start()

  private def savePlayers(): Unit = {
    for d <- players.values do {
      val p = d.player
      worldProvider.savePlayerData(Nbt.encode(p), p.id)
    }
  }

  private def saveWorldInfo(): Unit = {
    worldProvider.saveWorldData(Nbt.encode(world.worldInfo))
  }

  private def playerEffectiveViscosity(player: Player): Double = {
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
      for (playerId, p) <- players if p.shouldBeKicked do {
        logoutPlayer(p)
      }
      players.filterInPlace((_, p) => !p.shouldBeKicked)

      for (playerId, p) <- players do {
        if System.currentTimeMillis - p.lastSeen > 1000 then {
          p.shouldBeKicked = true
        }

        val PlayerData(player, entity, camera) = p
        val playerCoords = CoordUtils.approximateIntCoords(CylCoords(player.position).toBlockCoords)

        chunksLoadedPerPlayer.synchronized {
          chunksLoadedPerPlayer
            .get(player.id)
            .foreach(_.tick(Pose(CylCoords(camera.view.position), camera.view.forward)))
        }

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
        entity.headDirection.foreach(_.direction.set(player.rotation.x, 0, 0))
      }

      val tickResult = chunksLoadedPerPlayer.synchronized {
        chunksLoadCount.synchronized {
          val tickResult = world.tick(
            players.values.map(_.camera).toSeq,
            SeqUtils.roundRobin(chunksLoadedPerPlayer.values.map(_.nextAddableChunks(15)).toSeq),
            chunksLoadCount.filter((coords, count) => count == 0).keys.map(ChunkRelWorld(_)).toSeq
          )
          for coords <- tickResult.chunksRemoved do {
            chunksLoadCount.remove(coords.value)
          }
          tickResult
        }
      }

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
              p2.entity.headDirection.foreach { comp =>
                p.entityEventsWaitingToBeSent += p2.entity.id -> EntityEvent.HeadDirection(comp.direction)
              }
            }
          }
        }
      }
    } catch {
      case e: ZMQException => println(e)
      case e               => throw e
    }
  }

  private def performLeftMouseClick(player: Player, playerCamera: Camera): Unit = {
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

  private def performRightMouseClick(player: Player, playerCamera: Camera): Unit = {
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

  private def shutdown(): Unit = {
    isShuttingDown = true

    // give clients a chance to logout
    for _ <- 1 to 100 if players.nonEmpty do {
      Thread.sleep(10)
    }
  }

  def unload(): Unit = {
    shutdown()
    stop()

    savePlayers()
    saveWorldInfo()

    world.unload()
  }

  private def run(): Unit = {
    val messagesToSend: mutable.ArrayBuffer[(Long, Nbt)] = mutable.ArrayBuffer.empty

    while server.running do {
      try {
        server.receive() match {
          case Result.Ok((clientId, packet)) =>
            handlePacket(clientId, packet) match { // TODO: run this from the `tick` method to prevent race conditions
              case Some(res) =>
                messagesToSend += clientId -> res
              case None =>
            }
          case Result.Err(error) =>
            error match {
              case Error.InvalidPacket(message) =>
                // Ignore the invalid packet, since it's up to the client to send correct data
                println(s"Received invalid packet: $message")
            }
        }

        if server.running then {
          for (clientId, data) <- messagesToSend do {
            server.send(clientId, data) match {
              case Result.Ok(_)                             =>
              case Result.Err(Error.InvalidPacket(message)) =>
                // This is a bug in the server, not invalid input, so it's best to shut down
                throw new RuntimeException(s"Tried to send invalid packet: $message")
            }
          }
          messagesToSend.clear()
        }
      } catch {
        case _: InterruptedException =>
      }
    }

    server.unload()
  }

  private def logoutPlayer(playerData: PlayerData): Unit = {
    val player = playerData.player
    worldProvider.savePlayerData(Nbt.encode(player), player.id)
    world.removeEntity(playerData.entity)

    chunksLoadedPerPlayer.synchronized {
      chunksLoadedPerPlayer.remove(player.id) match {
        case Some(prio) =>
          while prio.nextRemovableChunk.isDefined do {
            val coords = prio.popChunkToRemove().get
            chunksLoadCount.synchronized {
              chunksLoadCount(coords.value) -= 1
            }
          }
        case None =>
      }
    }

    val name = player.name
    for otherPlayer <- players do {
      val (playerId, otherData) = otherPlayer
      otherData.messagesWaitingToBeSent.synchronized {
        otherData.messagesWaitingToBeSent += ServerMessage(s"$name logged out", ServerMessage.Sender.Server)
      }
    }
  }

  private val chunksLoadedPerPlayer: mutable.HashMap[UUID, ChunkLoadingPrioritizer] = mutable.HashMap.empty
  private val chunksLoadCount = mutable.LongMap.empty[Int]

  private var hasSentServerStartMessage: Boolean = false

  private def handlePacket(clientId: Long, packet: NetworkPacket): Option[Nbt.MapTag] = {
    import NetworkPacket.*

    // TODO: call this function from tick to reduce race conditions

    packet match {
      case Login(id, name) =>
        if isShuttingDown then {
          return Some(
            Nbt.makeMap(
              "success" -> Nbt.ByteTag(false),
              "error" -> Nbt.StringTag("server is shutting down")
            )
          )
        } else if !players.contains(clientId) then {
          if !isOnline && players.nonEmpty then {
            return Some(
              Nbt.makeMap(
                "success" -> Nbt.ByteTag(false),
                "error" -> Nbt.StringTag("only one player may join an offline world")
              )
            )
          }

          if players.map(_._2.player.id).exists(_ == id) then {
            return Some(
              Nbt.makeMap(
                "success" -> Nbt.ByteTag(false),
                "error" -> Nbt.StringTag("a player has already logged in with that id")
              )
            )
          }

          val playerNbt = worldProvider.loadPlayerData(id).orNull
          val player = if playerNbt != null then {
            Player.fromNBT(id, name, playerNbt)
          } else {
            makePlayer(id, name, world)
          }
          worldProvider.savePlayerData(Nbt.encode(player), player.id)

          val entity = Entity(
            Entity.getNextId,
            "player",
            Seq(
              TransformComponent(CylCoords(player.position).offset(-2, -2, -1)),
              MotionComponent(),
              HeadDirectionComponent(Vector3d(player.rotation.x, 0, 0)),
              BoundsComponent(Entity.playerBounds)
            )
          )
          val camera = new Camera(CameraProjection(70f, 16f / 9f, 0.02f, 100000f))
          val playerData = PlayerData(player, entity, camera)
          players(clientId) = playerData

          if !hasSentServerStartMessage && isOnline then {
            hasSentServerStartMessage = true
            val address = server.localAddress
            val port = server.localPort
            val message = s"Server started on $address:$port"
            playerData.messagesWaitingToBeSent += ServerMessage(message, ServerMessage.Sender.Server)
          }

          for otherPlayer <- players do {
            val (playerId, otherData) = otherPlayer
            if playerId != clientId then {
              otherData.entityEventsWaitingToBeSent.synchronized {
                otherData.entityEventsWaitingToBeSent += entity.id -> EntityEvent.Spawned(Nbt.encode(entity))
              }
              playerData.entityEventsWaitingToBeSent.synchronized {
                playerData.entityEventsWaitingToBeSent += otherData.entity.id -> EntityEvent.Spawned(
                  Nbt.encode(otherData.entity)
                )
              }
              otherData.messagesWaitingToBeSent.synchronized {
                otherData.messagesWaitingToBeSent += ServerMessage(s"$name logged in", ServerMessage.Sender.Server)
              }
            }
          }
          // println("Received login message from a new client")
          return Some(Nbt.makeMap("success" -> Nbt.ByteTag(true)))
        } else {
          println("Received login message from a logged in client")
          return Some(
            Nbt.makeMap(
              "success" -> Nbt.ByteTag(false),
              "error" -> Nbt.StringTag("already logged in") // Maybe it's better to ignore the message?
            )
          )
        }
      case GetWorldInfo =>
        val info = worldProvider.getWorldInfo
        return Some(Nbt.encode(info))
      case _ =>
        if !players.contains(clientId) then {
          println("Received message from unknown client")
          return None // the client is not logged in
        }
    }

    val playerData = players(clientId)
    playerData.lastSeen = System.currentTimeMillis

    val PlayerData(player, _, playerCamera) = playerData

    packet match {
      case Login(_, _) => None // already handled above
      case Logout =>
        logoutPlayer(playerData)
        players.remove(clientId)
        None
      case GetWorldInfo => None // already handled above
      case LoadColumnData(coords) =>
        world.getColumn(coords) match {
          case Some(column) => Some(Nbt.encode(ChunkColumnData(Some(column.terrainHeight))))
          case None         => Some(Nbt.emptyMap) // TODO: return None
        }
      case LoadWorldData =>
        Some(Nbt.encode(world.worldInfo))
      case GetPlayerState =>
        Some(Nbt.encode(player))
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
            case EntityEvent.Spawned(_)       => "spawned"
            case EntityEvent.Despawned        => "despawned"
            case EntityEvent.Position(_)      => "position"
            case EntityEvent.Rotation(_)      => "rotation"
            case EntityEvent.Velocity(_)      => "velocity"
            case EntityEvent.Flying(_)        => "flying"
            case EntityEvent.HeadDirection(_) => "head_direction"
          }

          val extraFields: Seq[(String, Nbt)] = e match {
            case EntityEvent.Spawned(data)    => Seq("data" -> data)
            case EntityEvent.Despawned        => Seq()
            case EntityEvent.Position(pos)    => Seq("pos" -> Nbt.makeVectorTag(pos.toVector3d))
            case EntityEvent.Rotation(r)      => Seq("r" -> Nbt.makeVectorTag(r))
            case EntityEvent.Velocity(v)      => Seq("v" -> Nbt.makeVectorTag(v))
            case EntityEvent.Flying(f)        => Seq("f" -> Nbt.ByteTag(f))
            case EntityEvent.HeadDirection(d) => Seq("d" -> Nbt.makeVectorTag(d))
          }

          Nbt.makeMap(extraFields*).withField("type", Nbt.StringTag(name))
        }

        val messages = playerData.messagesWaitingToBeSent.synchronized {
          val messages = playerData.messagesWaitingToBeSent.toSeq
          playerData.messagesWaitingToBeSent.clear()
          messages
        }

        val response = Nbt.emptyMap
          .withField("block_updates", Nbt.ListTag(updatesNbt))
          .withField("entity_events", Nbt.makeMap("ids" -> Nbt.ListTag(ids), "events" -> Nbt.ListTag(events)))
          .withField("server_shutting_down", Nbt.ByteTag(isShuttingDown)) // TODO: make proper shutdown feature
          .withField("messages", Nbt.ListTag(messages.map(Nbt.encode)))

        Some(response)
      case GetWorldLoadingEvents(maxChunksToLoad) =>
        val prio = chunksLoadedPerPlayer.synchronized {
          chunksLoadedPerPlayer.getOrElseUpdate(player.id, ChunkLoadingPrioritizer(world.renderDistance))
        }

        val loadedChunks = mutable.ArrayBuffer.empty[(ChunkRelWorld, Nbt)]
        val unloadedChunks = mutable.ArrayBuffer.empty[ChunkRelWorld]

        var chunksToLoad = maxChunksToLoad
        while chunksToLoad > 0 do {
          chunksToLoad -= 1

          prio.nextAddableChunk.flatMap(coords => world.getChunk(coords).map(coords -> _)) match {
            case Some(coords -> chunk) =>
              loadedChunks += ((coords, Nbt.encode(chunk)))
              prio += coords
              chunksLoadCount.synchronized {
                chunksLoadCount(coords.value) = chunksLoadCount.getOrElse(coords.value, 0) + 1
              }
            case None =>
              chunksToLoad = 0
          }
        }

        var moreChunksToUnload = true
        while moreChunksToUnload do {
          prio.popChunkToRemove() match {
            case Some(coords) =>
              unloadedChunks += coords
              chunksLoadCount.synchronized {
                chunksLoadCount(coords.value) -= 1
              }
            case None =>
              moreChunksToUnload = false
          }
        }

        Some(
          Nbt.makeMap(
            "chunks_loaded" -> Nbt.ListTag(
              loadedChunks
                .map((coords, data) => Nbt.makeMap("coords" -> Nbt.LongTag(coords.value), "data" -> data))
                .toSeq
            ),
            "chunks_unloaded" -> Nbt.ListTag(
              unloadedChunks
                .map(coords => Nbt.LongTag(coords.value))
                .toSeq
            )
          )
        )
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
        Some(Nbt.encode(inv))
      case PlayerMovedMouse(dist) =>
        playerData.mouseMovement.add(dist)
        None
      case PlayerPressedKeys(keys) =>
        playerData.pressedKeys = keys
        None
      case RunCommand(command, args) =>
        command match {
          case "chat" =>
            if args.length != 1 then {
              println(s"Wrong number of arguments to chat command: ${args.length}")
              return None
            }
            val message = args.head

            for p <- players.values do {
              p.messagesWaitingToBeSent.synchronized {
                p.messagesWaitingToBeSent += ServerMessage(
                  text = message,
                  sender = ServerMessage.Sender.Player(player.id, player.name)
                )
              }
            }
          case "spawn" =>
            if args.length != 4 then {
              println(s"Wrong number of arguments to spawn command: ${args.length}")
              return None
            }
            val entityType = args.head
            val pos = CylCoords(args(1).toDouble, args(2).toDouble, args(3).toDouble)

            Entity.atStartPos(Entity.getNextId, pos, entityType) match {
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

  private def makePlayer(id: UUID, name: String, world: ServerWorld): Player = {
    given CylinderSize = world.size

    val startX = (math.random() * 10 - 5).toInt
    val startZ = (math.random() * 10 - 5).toInt
    val startY = world.getHeight(startX, startZ) + 4
    Player.atStartPos(id, name, BlockCoords(startX, startY, startZ).toCylCoords)
  }

  private def stop(): Unit = {
    server.stop()
    serverThread.join()
  }
}
