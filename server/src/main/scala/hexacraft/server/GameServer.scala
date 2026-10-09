package hexacraft.server

import hexacraft.game.*
import hexacraft.nbt.Nbt
import hexacraft.server.TcpServer.Error
import hexacraft.server.entity.EntitySpawnEvent
import hexacraft.server.world.{ChunkLoadingPrioritizer, EntityFactory, ServerWorld, WorldProvider}
import hexacraft.util.{Result, SeqUtils}
import hexacraft.world.*
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.{Chunk, ChunkColumnData, ChunkData}
import hexacraft.world.coord.*
import hexacraft.world.entity.*

import org.joml.{Vector2f, Vector3d}

import java.util.UUID
import scala.collection.mutable

object GameServer {
  def create(
      isOnline: Boolean,
      port: Int,
      worldInfo: WorldInfo,
      worldProvider: WorldProvider,
      renderDistance: Double,
      maxChunksToLoadPerTick: Int = 4,
      enableWaterPhysics: Boolean = true
  ): GameServer = {
    val world = new ServerWorld(worldProvider, worldInfo, renderDistance, maxChunksToLoadPerTick, enableWaterPhysics)

    val tcpServer = TcpServer
      .start(port)
      .unwrapWith(m => new IllegalStateException(s"Could not start server: $m"))

    val lanAnnouncer = Option.when(isOnline)(LanDiscovery.Announcer.start(port, worldInfo.worldName))

    new GameServer(isOnline, tcpServer, lanAnnouncer, worldInfo, worldProvider, world)(using world.size)
  }
}

class GameServer(
    isOnline: Boolean,
    server: TcpServer,
    lanAnnouncer: Option[LanDiscovery.Announcer],
    worldInfo: WorldInfo,
    worldProvider: WorldProvider,
    world: ServerWorld
)(using CylinderSize) {

  /** How far away (in blocks) the player can reach when clicking on blocks and entities */
  private val ReachDistance: Int = 7

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

  def tick(): Unit = {
    try {
      val players = this.players.synchronized {
        for (playerId, p) <- this.players if p.shouldBeKicked do {
          logoutPlayer(p)
        }
        this.players.filterInPlace((_, p) => !p.shouldBeKicked)
        this.players.clone()
      }

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
          val isInFluid = PlayerPhysicsHandler.playerEffectiveViscosity(player, world) > Block.Air.viscosity.toSI * 2

          val mounts = this.world.entitiesMountedBy(player.id)

          playerInputHandler.tick(
            player,
            p.pressedKeys,
            p.mouseMovement,
            maxSpeed,
            isInFluid,
            mounts
          )
          p.mouseMovement.set(0)

          playerPhysicsHandler.tick(
            player,
            maxSpeed,
            PlayerPhysicsHandler.playerEffectiveViscosity(player, world),
            PlayerPhysicsHandler.playerVolumeSubmergedInWater(player, world),
            mounts
          )

          if mounts.nonEmpty && p.pressedKeys.contains(GameKeyboard.Key.Sneak) then {
            val mount = mounts.head
            world.removeEntity(mount)
            world.addEntity(mount.withoutComponents {
              case c: MountComponent => c.mountedEntity == player.id
              case _                 => false
            })
            player.position.y += 1
          }
        }

        camera.setPositionAndRotation(player.position, player.rotation)
        camera.updateCoords()
        camera.updateViewMatrix()

        entity.transform.position = CylCoords(player.position)
          .offset(0, player.bounds.bottom.toDouble, 0)
        entity.transform.rotation.set(entityRotationFacingLikePlayer(player))
        entity.motion.velocity.set(player.velocity)
        entity.motion.flying = player.flying
        entity.accessComponent { case c: HeadDirectionComponent =>
          c.direction.set(player.rotation.x, 0, 0)
        }
      }

      val tickResult = chunksLoadedPerPlayer.synchronized {
        chunksLoadCount.synchronized {
          val tickResult = world.tick(
            players.values.map(_.camera).toSeq,
            SeqUtils.roundRobin(chunksLoadedPerPlayer.values.map(_.nextAddableChunks(50)).toSeq),
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
              p2.entity.accessComponent { case c: HeadDirectionComponent =>
                p.entityEventsWaitingToBeSent += p2.entity.id -> EntityEvent.HeadDirection(c.direction)
              }
            }
          }
        }
      }
    } catch {
      case e: NetworkException => println(e)
      case e                   => throw e
    }
  }

  private def performLeftMouseClick(player: Player, proj: CameraProjection): Unit = {
    val camera = cameraForPlayer(player, proj)
    val ray = Ray.fromScreen(camera, Vector2f(0, 0)).get

    findClosestHit(camera, ray) match {
      case Some(Hit.OnBlock(coords, state, _)) =>
        if state.blockType != Block.Air then {
          world.removeBlock(coords)
          notifyPlayersAboutBlockUpdate(coords, BlockState.Air)
        }
      case _ => // nothing to do (yet) when clicking on an entity
    }
  }

  private def notifyPlayersAboutBlockUpdate(coords: BlockRelWorld, blockState: BlockState): Unit = {
    for (_, playerData) <- players do {
      playerData.blockUpdatesWaitingToBeSent.synchronized {
        playerData.blockUpdatesWaitingToBeSent += coords -> blockState
      }
    }
  }

  private def performRightMouseClick(player: Player, proj: CameraProjection): Unit = {
    val camera = cameraForPlayer(player, proj)
    val ray = Ray.fromScreen(camera, Vector2f(0, 0)).get
    val closestHit = findClosestHit(camera, ray)

    closestHit match {
      case Some(Hit.OnEntity(entity)) =>
        entity.typeName match {
          case "boat" =>
            world.removeEntity(entity)
            world.addEntity(entity.withComponent(MountComponent(player.id)))

            // Look in the forward direction of the boat
            player.rotation.y = PlayerInputHandler.wrapAngle(-entity.transform.rotation.y)
            if player.rotation.y < 0 then {
              player.rotation.y += math.Pi * 2
            }
          case t =>
            println(s"Clicked on entity of type $t")
        }
      case Some(Hit.OnBlock(coords, state, Some(side))) =>
        val coordsInFront = coords.offset(NeighborOffsets(side))

        state.blockType match {
          case Block.Tnt => explode(coords)
          case _         => tryPlacingBlockAt(coordsInFront, player)
        }
      case _ =>
    }
  }

  /** Finds the closest block or entity that the ray hits within reach */
  private def findClosestHit(camera: Camera, ray: Ray): Option[Hit] = {
    val candidates = Seq(findClosestEntity(camera, ray), findClosestBlock(camera, ray)).flatten
    candidates.minByOption(_._2).map(_._1)
  }

  /** Finds the closest entity that the ray hits within reach, together with the distance to it */
  private def findClosestEntity(camera: Camera, ray: Ray): Option[(Hit, Double)] = {
    world
      .filterMapEntities { e =>
        val coords = e.transform.position.toBlockCoords
        val points = PointHexagon.fromHexBox(e.boundingBox, coords, camera)
        points
          .distanceToBox(ray)
          .filter(_ < ReachDistance * CylinderSize.y60) // convert unit from blocks to meters
      }
      .map((e, d) => (Hit.OnEntity(e), d))
      .minByOption(_._2)
  }

  /** Finds the closest solid block that the ray hits within reach, together with the distance to it */
  private def findClosestBlock(camera: Camera, ray: Ray): Option[(Hit, Double)] = {
    new RayTracer(camera, ReachDistance)
      .trace(ray, c => Some(world.getBlock(c)).filter(_.blockType.isSolid))
      .flatMap { case (coords, side) =>
        val block = world.getBlock(coords)
        val bounds = block.blockType.bounds(block.metadata)
        val points = PointHexagon.fromHexBox(bounds, BlockCoords(coords), camera)
        points.distanceToBox(ray).map((Hit.OnBlock(coords, block, side), _))
      }
  }

  private def cameraForPlayer(player: Player, proj: CameraProjection) = {
    val c = Camera(proj)
    c.setPositionAndRotation(player.position, player.rotation)
    c.updateCoords()
    c.updateViewMatrix()
    c
  }

  /** The rotation an entity needs to face the same way as the player */
  private def entityRotationFacingLikePlayer(player: Player): Vector3d =
    Vector3d(0, -player.rotation.y, 0)

  private def tryPlacingBlockAt(coords: BlockRelWorld, player: Player): Unit = {
    if world.getBlock(coords).blockType.isSolid then {
      return
    }

    val blockType = player.blockInHand
    if blockType == Block.Air then return

    val state = new BlockState(blockType)

    val collides = world.collisionDetector.collides(
      blockType.bounds(state.metadata),
      BlockCoords(coords).toCylCoords,
      player.bounds,
      CylCoords(player.position)
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
            id,
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
                otherData.entityEventsWaitingToBeSent += entity.id -> EntitySpawnEvent.of(entity)
              }
              playerData.entityEventsWaitingToBeSent.synchronized {
                playerData.entityEventsWaitingToBeSent += otherData.entity.id -> EntitySpawnEvent.of(otherData.entity)
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
        return Some(Nbt.encode(worldInfo))
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
          case Some(column) => Some(Nbt.encode(ChunkColumnData(column)))
          case None         => Some(Nbt.emptyMap) // TODO: return None
        }
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

        val events = for (_, e) <- entityEvents yield Nbt.encode(e)

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
              // The entities are sent separately from the chunk
              loadedChunks += ((coords, ChunkData.encode(chunk.chunkData, includeEntities = false)))
              playerData.entityEventsWaitingToBeSent.synchronized {
                for e <- chunk.entities do {
                  playerData.entityEventsWaitingToBeSent += e.id -> EntitySpawnEvent.of(e)
                }
              }
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
              // The chunk is still loaded by the server since this player is still counted as one of its users
              playerData.entityEventsWaitingToBeSent.synchronized {
                for chunk <- world.getChunk(coords); e <- chunk.entities do {
                  playerData.entityEventsWaitingToBeSent += e.id -> EntityEvent.Despawned
                }
              }
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
        performRightMouseClick(player, playerCamera.proj)
        None
      case PlayerLeftClicked =>
        performLeftMouseClick(player, playerCamera.proj)
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
                  sender = ServerMessage.Sender.Player(player.name)
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

            EntityFactory.atStartPos(
              Entity.getNextId,
              pos,
              entityType,
              entityRotationFacingLikePlayer(player)
            ) match {
              case Result.Ok(entity) =>
                world.addEntity(entity)
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
    lanAnnouncer.foreach(_.close())
    server.stop()
    serverThread.join()
  }
}

/** Something the player's crosshair points at */
private enum Hit {
  case OnBlock(coords: BlockRelWorld, state: BlockState, side: Option[Int])
  case OnEntity(entity: Entity)
}
