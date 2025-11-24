package hexacraft.client

import hexacraft.client.render.{FarDistanceTerrainRenderer, StandardTerrainRenderer, TerrainRenderer}
import hexacraft.game.*
import hexacraft.gui.*
import hexacraft.gui.comp.Component
import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.audio.AudioSystem.BufferId
import hexacraft.infra.window.{KeyAction, KeyboardKey, MouseAction, MouseButton}
import hexacraft.math.MathUtils
import hexacraft.nbt.Nbt
import hexacraft.renderer.{Renderer, TextureArray, VAO}
import hexacraft.shaders.CrosshairShader
import hexacraft.util.{Channel, NamedThreadFactory, Result, TickableTimer}
import hexacraft.world.*
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.{Chunk, ChunkColumnData, ChunkColumnTerrain}
import hexacraft.world.coord.*

import org.joml.{Matrix4f, Vector2f, Vector3d, Vector3f}
import org.zeromq.*

import java.util.UUID
import java.util.concurrent.{Executors, TimeUnit}
import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future, TimeoutException}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.Random

object GameClient {
  enum Event {
    case GameQuit
    case CursorCaptured
    case CursorReleased
  }

  private val useFarDistanceRenderer = false

  def create(
      playerId: UUID,
      playerName: String,
      serverIp: String,
      serverPort: Int,
      isOnline: Boolean,
      keyboard: GameKeyboard,
      blockLoader: BlockTextureLoader,
      initialWindowSize: WindowSize,
      audioSystem: AudioSystem
  ): Result[(GameClient, Channel.Receiver[GameClient.Event]), String] = {
    val socket = GameClientSocket(serverIp, serverPort)

    val (worldInfo, player) = fetchWorldInfo(socket, playerId, playerName) match {
      case Result.Ok(value) => value
      case Result.Err(message) =>
        socket.close()
        return Result.Err(s"failed to connect to server: $message")
    }

    val blockSpecs = BlockSpecs.default
    val blockTextureMapping = BlockTextureLoader.loadBlockTextures(blockSpecs, blockLoader).unwrap()
    val blockTextureIndices: Map[String, IndexedSeq[Int]] =
      blockSpecs.view.mapValues(spec => spec.textures.indices(blockTextureMapping.texIdxMap)).toMap
    val blockTextureColors: Map[String, IndexedSeq[Vector3f]] =
      blockTextureIndices.view
        .mapValues(indices => indices.map(idx => blockTextureMapping.images(idx & 0xfff).averageColor))
        .toMap

    TextureArray.registerTextureArray("blocks", 32, blockTextureMapping.images)

    val crosshairShader = new CrosshairShader()
    val crosshairVAO: VAO = CrosshairShader.createVao()
    val crosshairRenderer: Renderer = CrosshairShader.createRenderer()

    val world = ClientWorld(worldInfo)

    given CylinderSize = world.size

    val terrainRenderer: TerrainRenderer =
      if useFarDistanceRenderer then {
        FarDistanceTerrainRenderer(world.worldGenerator, blockTextureColors)
      } else {
        StandardTerrainRenderer(world, blockTextureIndices)
      }

    val worldRenderer: WorldRenderer = new WorldRenderer(world, initialWindowSize.physicalSize, terrainRenderer)

    val camera: Camera = new Camera(makeCameraProjection(initialWindowSize, world.size.worldSize))

    val playerInputHandler: PlayerInputHandler = new PlayerInputHandler
    val playerPhysicsHandler: PlayerPhysicsHandler = new PlayerPhysicsHandler(world.collisionDetector)

    val toolbar: Toolbar = makeToolbar(player, initialWindowSize, blockTextureIndices)
    val blockInHandRenderer: GuiBlockRenderer =
      makeBlockInHandRenderer(world, camera, blockTextureIndices, initialWindowSize)

    val placeBlockSoundBuffer = audioSystem.loadSoundBuffer("sounds/place_block.ogg")
    val destroyBlockSoundBuffer = audioSystem.loadSoundBuffer("sounds/place_block.ogg")
    val walkSoundBuffer1 = audioSystem.loadSoundBuffer("sounds/walk1.ogg")
    val walkSoundBuffer2 = audioSystem.loadSoundBuffer("sounds/walk2.ogg")

    val (tx, rx) = Channel[Event]()

    val client = new GameClient(
      socket,
      isOnline,
      audioSystem,
      tx,
      blockTextureIndices,
      crosshairShader,
      crosshairVAO,
      crosshairRenderer,
      world,
      player,
      worldRenderer,
      camera,
      playerInputHandler,
      playerPhysicsHandler,
      keyboard,
      toolbar,
      blockInHandRenderer,
      placeBlockSoundBuffer,
      destroyBlockSoundBuffer,
      walkSoundBuffer1,
      walkSoundBuffer2
    )

    client.updateBlockInHandRendererContent()
    client.setUniforms(initialWindowSize.logicalAspectRatio)
    client.setUseMouse(true)

    Result.Ok((client, rx))
  }

  private def fetchWorldInfo(
      socket: GameClientSocket,
      playerId: UUID,
      playerName: String
  ): Result[(WorldInfo, Player), String] = try {
    val loginResponse = socket.sendPacketAndWait(NetworkPacket.Login(playerId, playerName)).asMap.get
    val loginSuccessful = loginResponse.getBoolean("success", false)
    if !loginSuccessful then {
      val errorMessage = loginResponse.getString("error", "")
      return Result.Err(s"failed to login: $errorMessage")
    }

    val worldInfo = WorldInfo.fromNBT(
      socket.sendPacketAndWait(NetworkPacket.GetWorldInfo).asMap.get,
      null,
      WorldSettings.none
    )

    val playerNbt = socket.sendPacketAndWait(NetworkPacket.GetPlayerState)
    val player = Player.fromNBT(playerId, playerName, playerNbt.asMap.get)

    Result.Ok((worldInfo, player))
  } catch {
    case e: Exception =>
      Result.Err(e.getMessage)
  }

  private def makeBlockInHandRenderer(
      world: ClientWorld,
      camera: Camera,
      blockTextureIndices: Map[String, IndexedSeq[Int]],
      initialWindowSize: WindowSize
  ): GuiBlockRenderer = {
    val renderer = GuiBlockRenderer(1, 1)(blockTextureIndices)
    renderer.setViewMatrix(makeBlockInHandViewMatrix)
    renderer.setWindowAspectRatio(initialWindowSize.logicalAspectRatio)
    renderer
  }

  private def makeToolbar(
      player: Player,
      windowSize: WindowSize,
      blockTextureIndices: Map[String, IndexedSeq[Int]]
  ): Toolbar = {
    val location = LocationInfo(-4.5f * 0.2f, -0.83f - 0.095f, 2 * 0.9f, 2 * 0.095f)

    val toolbar = new Toolbar(location, player.inventory)(blockTextureIndices)
    toolbar.setSelectedIndex(player.selectedItemSlot)
    toolbar.setWindowAspectRatio(windowSize.logicalAspectRatio)
    toolbar
  }

  private def makeCameraProjection(windowSize: WindowSize, worldSize: Int) = {
    val far = worldSize match {
      case 0 => 100000f
      case 1 => 10000f
      case _ => 1000f
    }

    new CameraProjection(70f, windowSize.logicalAspectRatio, 0.02f, far)
  }

  private def makeBlockInHandViewMatrix = {
    new Matrix4f()
      .translate(0, 0, -2f)
      .rotateZ(-3.1415f / 12)
      .rotateX(3.1415f / 6)
      .translate(0, -0.25f, 0)
  }
}

class GameClient(
    socket: GameClientSocket,
    isOnline: Boolean,
    audioSystem: AudioSystem,
    eventHandler: Channel.Sender[GameClient.Event],
    blockTextureIndices: Map[String, IndexedSeq[Int]],
    crosshairShader: CrosshairShader,
    crosshairVAO: VAO,
    crosshairRenderer: Renderer,
    world: ClientWorld,
    val player: Player,
    worldRenderer: WorldRenderer,
    val camera: Camera,
    playerInputHandler: PlayerInputHandler,
    playerPhysicsHandler: PlayerPhysicsHandler,
    keyboard: GameKeyboard,
    toolbar: Toolbar,
    blockInHandRenderer: GuiBlockRenderer,
    placeBlockSoundBuffer: AudioSystem.BufferId,
    destroyBlockSoundBuffer: AudioSystem.BufferId,
    walkSoundBuffer1: AudioSystem.BufferId,
    walkSoundBuffer2: AudioSystem.BufferId
)(using CylinderSize) {
  private val executorService = Executors.newFixedThreadPool(4, NamedThreadFactory("client"))
  given ExecutionContext = ExecutionContext.fromExecutor(executorService)

  private var moveWithMouse: Boolean = false
  private var isPaused: Boolean = false
  private var isInPopup: Boolean = false
  private var isLoggingOut: Boolean = false
  private var shouldOpenChat: Boolean = false

  private val chatMessagesToSend: mutable.ArrayBuffer[String] = mutable.ArrayBuffer.empty

  private var debugOverlay: Option[DebugOverlay] = None
  private val chatOverlay: ChatOverlay = makeChatOverlay()

  private var selectedBlockAndSide: Option[MousePickerResult] = None
  private val overlays: mutable.ArrayBuffer[Component] = mutable.ArrayBuffer(chatOverlay)

  private val rightMouseButtonTimer: TickableTimer = TickableTimer(10, initEnabled = false)
  private val leftMouseButtonTimer: TickableTimer = TickableTimer(10, initEnabled = false)
  private val walkSoundTimer: TickableTimer = TickableTimer(20, initEnabled = false)

  def isReadyToPlay: Boolean = {
    this.world.getChunk(this.camera.blockCoords.getChunkRelWorld).isDefined
  }

  private def makeChatOverlay(): ChatOverlay = {
    val (tx, rx) = Channel[ChatOverlay.Event]()
    val chat = ChatOverlay(tx)
    rx.onEvent {
      case ChatOverlay.Event.Closed =>
        chat.setInputEnabled(false)
        this.isInPopup = false
        this.setUseMouse(true)
      case ChatOverlay.Event.MessageSubmitted(message) =>
        if message.nonEmpty then {
          this.chatMessagesToSend += message
        }
        chat.setInputEnabled(false)
        this.isInPopup = false
        this.setUseMouse(true)
    }
    chat
  }

  private def setUniforms(windowAspectRatio: Float): Unit = {
    setProjMatrixForAll()
    worldRenderer.onTotalSizeChanged(world.size.totalSize)
    crosshairShader.setWindowAspectRatio(windowAspectRatio)
  }

  private def setProjMatrixForAll(): Unit = {
    worldRenderer.onProjMatrixChanged(camera)
  }

  private def pauseGame(): Unit = {
    import PauseMenu.Event.*

    val (tx, rx) = Channel[PauseMenu.Event]()
    val pauseMenu = PauseMenu(tx)

    rx.onEvent {
      case Unpause =>
        overlays -= pauseMenu
        pauseMenu.unload()
        setPaused(false)
      case QuitGame =>
        logout()
    }

    overlays += pauseMenu
    setPaused(true)
  }

  private def logout(): Future[Unit] = {
    if isLoggingOut then {
      return Future.unit
    }
    if isOnline then {
      println("Logging out...")
    }
    isLoggingOut = true
    val logoutFuture = Future(socket.sendPacket(NetworkPacket.Logout)) // use Future to make sure it does not block
    eventHandler.send(GameClient.Event.GameQuit)
    logoutFuture
  }

  private def handleKeyPress(key: KeyboardKey): Unit = key match {
    case KeyboardKey.Escape =>
      pauseGame()
    case KeyboardKey.Letter('E') =>
      import InventoryBox.Event.*

      if !isPaused then {
        val (tx, rx) = Channel[InventoryBox.Event]()
        val inventoryScene = InventoryBox(player.inventory, blockTextureIndices, tx)

        rx.onEvent {
          case BoxClosed =>
            overlays -= inventoryScene
            inventoryScene.unload()
            isInPopup = false
            setUseMouse(true)
          case InventoryUpdated(inv) =>
            val invNbt = socket.sendPacketAndWait(NetworkPacket.PlayerUpdatedInventory(inv))
            val serverInv = Nbt.decode[Inventory](invNbt.asMap.get).get
            player.inventory = serverInv
            toolbar.onInventoryUpdated(serverInv)
        }

        overlays += inventoryScene
        isInPopup = true
        setUseMouse(false)
      }
    case KeyboardKey.Letter('M') =>
      setUseMouse(!moveWithMouse)
    case KeyboardKey.Letter('T') =>
      shouldOpenChat = true
    case KeyboardKey.Letter('F') =>
      player.flying = !player.flying
      socket.sendPacket(NetworkPacket.PlayerToggledFlying)
    case KeyboardKey.Function(7) =>
      setDebugScreenVisible(debugOverlay.isEmpty)
    case KeyboardKey.Digit(digit) =>
      if digit > 0 then {
        setSelectedItemSlot(digit - 1)
      }
    case KeyboardKey.Letter('P') =>
      val pos = CylCoords(player.position)
      val spawnArgs = Seq("player", pos.x.toString, pos.y.toString, pos.z.toString)
      socket.sendPacket(NetworkPacket.RunCommand("spawn", spawnArgs))
    case KeyboardKey.Letter('L') =>
      val pos = CylCoords(player.position)
      val spawnArgs = Seq("sheep", pos.x.toString, pos.y.toString, pos.z.toString)
      socket.sendPacket(NetworkPacket.RunCommand("spawn", spawnArgs))
    case KeyboardKey.Letter('K') =>
      socket.sendPacket(NetworkPacket.RunCommand("kill", Seq("@e")))
    case _ =>
  }

  private def setDebugScreenVisible(visible: Boolean): Unit = {
    if visible then {
      if debugOverlay.isEmpty then {
        debugOverlay = Some(new DebugOverlay)
      }
    } else {
      if debugOverlay.isDefined then {
        debugOverlay.get.unload()
      }
      debugOverlay = None
    }
  }

  private def setUseMouse(useMouse: Boolean): Unit = {
    moveWithMouse = useMouse
    setMouseCursorInvisible(moveWithMouse)
  }

  def handleEvent(event: Event): Boolean = {
    if overlays.reverseIterator.exists(_.handleEvent(event)) then {
      return true
    }

    import Event.*
    event match {
      case KeyEvent(key, _, action, _) =>
        if action == KeyAction.Press then {
          handleKeyPress(key)
        }
      case ScrollEvent(_, yOffset, _) =>
        if !isPaused && !isInPopup && moveWithMouse then {
          val dy = -math.signum(yOffset).toInt
          if dy != 0 then {
            val slot = (player.selectedItemSlot + dy + 9) % 9
            setSelectedItemSlot(slot)
          }
        }
      case MouseClickEvent(button, action, _, _) =>
        button match {
          case MouseButton.Left  => leftMouseButtonTimer.enabled = action != MouseAction.Release
          case MouseButton.Right => rightMouseButtonTimer.enabled = action != MouseAction.Release
          case _                 =>
        }
      case _ =>
    }
    true
  }

  private def setSelectedItemSlot(itemSlot: Int): Unit = {
    player.selectedItemSlot = itemSlot
    updateBlockInHandRendererContent()
    toolbar.setSelectedIndex(itemSlot)
    socket.sendPacket(NetworkPacket.PlayerSetSelectedItemSlot(itemSlot.toShort))
  }

  private def updateBlockInHandRendererContent(): Unit = {
    blockInHandRenderer.updateContent(1.5f, -0.9f, Seq(player.blockInHand))
  }

  private def setPaused(paused: Boolean): Unit = {
    if isPaused == paused then {
      return
    }

    isPaused = paused
    setMouseCursorInvisible(!paused && moveWithMouse)
  }

  private def setMouseCursorInvisible(invisible: Boolean): Unit = {
    if invisible then {
      eventHandler.send(GameClient.Event.CursorCaptured)
    } else {
      eventHandler.send(GameClient.Event.CursorReleased)
    }
  }

  def windowFocusChanged(focused: Boolean): Unit = {
    if !focused then {
      if !isPaused && !chatOverlay.isInputEnabled then {
        pauseGame()
      }
    }
  }

  def windowResized(width: Int, height: Int): Unit = {
    val aspectRatio = width.toFloat / height
    camera.proj.aspect = aspectRatio
    camera.updateProjMatrix()

    setProjMatrixForAll()
    blockInHandRenderer.setWindowAspectRatio(aspectRatio)
    toolbar.setWindowAspectRatio(aspectRatio)

    crosshairShader.setWindowAspectRatio(aspectRatio)
  }

  def frameBufferResized(width: Int, height: Int): Unit = {
    worldRenderer.frameBufferResized(width, height)
  }

  def render(context: RenderContext): Unit = {
    worldRenderer.render(camera, new Vector3f(0, 1, -1), selectedBlockAndSide)

    renderCrosshair()

    blockInHandRenderer.render()
    toolbar.render(context)

    if debugOverlay.isDefined then {
      debugOverlay.get.render(context)
    }

    for s <- overlays do {
      s.render(context)
    }
  }

  private def renderCrosshair(): Unit = {
    if isPaused || isInPopup || !moveWithMouse then {
      return
    }

    crosshairShader.enable()
    crosshairRenderer.render(crosshairVAO, crosshairVAO.maxCount)
  }

  private def playSoundAt(sound: BufferId, coords: CylCoords): Unit = {
    val baseCoords = CylCoords.Offset(coords.toVector3d)

    // Since the world is wrapped we need to play three sounds (there might be a better solution...)
    val c = world.size.circumference
    for dz <- Seq(-c, 0, c) do {
      val sourceId = audioSystem.createSoundSource(sound)
      audioSystem.setSoundSourcePosition(sourceId, baseCoords.offset(0, 0, dz).toVector3f)
      audioSystem.startPlayingSound(sourceId)
    }
  }

  private var tickFut: Option[Future[Seq[Nbt]]] = None

  def tick(ctx: TickContext): Unit = {
    if isLoggingOut then return

    if shouldOpenChat then {
      shouldOpenChat = false
      chatOverlay.setInputEnabled(true)
      isInPopup = true
      setUseMouse(false)
    }

    if this.chatMessagesToSend.nonEmpty then {
      for m <- this.chatMessagesToSend do {
        socket.sendPacket(NetworkPacket.RunCommand("chat", Seq(m)))
      }
      this.chatMessagesToSend.clear()
    }

    // Act on the server info requested last tick, and send a new request to be used in the next tick
    val currentTickFut = tickFut
    tickFut = Some(Future {
      val packets = Seq(NetworkPacket.GetPlayerState, NetworkPacket.GetEvents, NetworkPacket.GetWorldLoadingEvents(5))
      socket.sendMultiplePacketsAndWait(packets)
    })
    if currentTickFut.isEmpty then return // the first tick has no server data to act on

    var serverIsShuttingDown = false

    try {
      val Seq(playerNbt, worldEventsNbtPacket, worldLoadingEventsNbt) =
        Await.result(currentTickFut.get, Duration(1, TimeUnit.SECONDS))

      val syncedPlayer = Player.fromNBT(player.id, player.name, playerNbt.asInstanceOf[Nbt.MapTag])
      // println(syncedPlayer.position)
      if player.position.sub(syncedPlayer.position, new Vector3d).length() > 10.0 then {}
      val positionDiff = syncedPlayer.position.sub(player.position, new Vector3d)
      positionDiff.z = MathUtils.absmin(positionDiff.z, world.size.circumference)
      player.position.add(positionDiff.mul(0.1))
      val rotationDiff = syncedPlayer.rotation.sub(player.rotation, new Vector3d)
      rotationDiff.y = MathUtils.absmin(rotationDiff.y, math.Pi * 2)
      //    player.rotation.add(rotationDiff.mul(0.1))
      player.flying = syncedPlayer.flying

      val worldEventsNbt = worldEventsNbtPacket.asMap.get
      if worldEventsNbt.getBoolean("server_shutting_down", false) then {
        println("The server is shutting down")
        serverIsShuttingDown = true
      }

      val messages = worldEventsNbt.getList("messages").getOrElse(Seq.empty)
      if messages.nonEmpty then {
        for m <- messages do {
          m.asMap.flatMap(Nbt.decode[ServerMessage]) match {
            case Some(m) =>
              this.chatOverlay.addMessage(m)
            case None =>
              println(s"Could not parse server message: $m")
          }
        }
      }

      val blockUpdatesNbtList = worldEventsNbt.getList("block_updates").getOrElse(Seq()).map(_.asMap.get)
      val blockUpdates =
        for u <- blockUpdatesNbtList
        yield {
          val coords = BlockRelWorld(u.getLong("coords", -1))
          val blockId = u.getByte("id", 0)
          val blockMeta = u.getByte("meta", 0)
          val blockState = BlockState(Block.byId(blockId), blockMeta)
          (coords, blockState)
        }

      for (coords, blockState) <- blockUpdates do {
        world.setBlock(coords, blockState)
      }

      val entityEventsNbt = worldEventsNbt.getMap("entity_events").get
      val entityEventIds = entityEventsNbt.getList("ids").get.map(_.asInstanceOf[Nbt.StringTag].v)
      val entityEventData = entityEventsNbt.getList("events").get.map(_.asMap.get)
      val entityEvents = for (id, eventNbt) <- entityEventIds.zip(entityEventData) yield {
        (UUID.fromString(id), Nbt.decode[EntityEvent](eventNbt).get)
      }

      updateSoundListener()

      val worldLoadingEvents = worldLoadingEventsNbt.asMap.get
      val loadedChunks = mutable.ArrayBuffer.empty[(ChunkRelWorld, Nbt)]
      for e <- worldLoadingEvents.getList("chunks_loaded").getOrElse(Seq()) do {
        val m = e.asMap.get
        val coords = m.getLong("coords", -1L)
        val data = m.getMap("data")
        if coords != -1L && data.isDefined then {
          loadedChunks += ChunkRelWorld(coords) -> data.get
        }
      }
      val unloadedChunks = mutable.ArrayBuffer.empty[ChunkRelWorld]
      for e <- worldLoadingEvents.getList("chunks_unloaded").getOrElse(Seq()) do {
        e match {
          case Nbt.LongTag(coords) =>
            unloadedChunks += ChunkRelWorld(coords)
          case _ =>
        }
      }

      for (chunkCoords, chunkNbt) <- loadedChunks do {
        var success = true
        val columnCoords = chunkCoords.getColumnRelWorld
        if world.getColumn(columnCoords).isEmpty then {
          val columnNbt = socket.sendPacketAndWait(NetworkPacket.LoadColumnData(columnCoords))
          if columnNbt != Nbt.emptyMap then {
            val column = ChunkColumnTerrain.create(
              columnCoords,
              world.worldGenerator,
              Some(Nbt.decode[ChunkColumnData](columnNbt.asInstanceOf[Nbt.MapTag]).get)
            )
            world.setColumn(columnCoords, column)
          } else {
            success = false
          }
        }
        if success && world.getChunk(chunkCoords).isEmpty then {
          if chunkNbt != Nbt.emptyMap then {
            val chunk = Nbt.decode[Chunk](chunkNbt.asInstanceOf[Nbt.MapTag]).get
            world.setChunk(chunkCoords, chunk)
          } else {
            success = false
          }
        }
        if !success then {
          println("Client got chunk from server, but was not ready to handle it")
        }
      }

      for chunkCoords <- unloadedChunks do {
        world.removeChunk(chunkCoords)
      }

      val playerCoords = CoordUtils.approximateIntCoords(CylCoords(player.position).toBlockCoords)

      if world.getChunk(playerCoords.getChunkRelWorld).isDefined then {
        val pressedKeys = keyboard.pressedKeys
        val maxSpeed = playerInputHandler.determineMaxSpeed(pressedKeys)
        if !isPaused && !isInPopup then {
          val mouseMovement = if moveWithMouse then ctx.mouseMovement else new Vector2f
          val isInFluid = PlayerPhysicsHandler.playerEffectiveViscosity(player, world) > Block.Air.viscosity.toSI * 2

          playerInputHandler.tick(player, pressedKeys, mouseMovement, maxSpeed, isInFluid)

          socket.sendPacket(NetworkPacket.PlayerMovedMouse(mouseMovement))
          socket.sendPacket(NetworkPacket.PlayerPressedKeys(pressedKeys))
        } else {
          socket.sendPacket(NetworkPacket.PlayerMovedMouse(Vector2f(0, 0)))
          socket.sendPacket(NetworkPacket.PlayerPressedKeys(Seq()))
        }

        if !isPaused || isOnline then {
          playerPhysicsHandler.tick(
            player,
            maxSpeed,
            PlayerPhysicsHandler.playerEffectiveViscosity(player, world),
            PlayerPhysicsHandler.playerVolumeSubmergedInWater(player, world)
          )
        }

        if !isPaused then {
          walkSoundTimer.enabled = !player.flying && player.velocity.y == 0 && player.velocity.lengthSquared() > 0.01
        }
      } else {
        socket.sendPacket(NetworkPacket.PlayerMovedMouse(Vector2f(0, 0)))
        socket.sendPacket(NetworkPacket.PlayerPressedKeys(Seq()))
      }

      if walkSoundTimer.tick() then {
        val sound = if math.random() < 0.5 then walkSoundBuffer1 else walkSoundBuffer2

        playSoundAt(sound, CylCoords(player.position).offset(0, player.bounds.bottom, 0))
      }

      camera.setPositionAndRotation(player.position, player.rotation)
      camera.updateCoords()
      camera.updateViewMatrix(camera.view.position)

      updateBlockInHandRendererContent()

      selectedBlockAndSide = updatedMousePicker(ctx.windowSize, ctx.currentMousePosition)

      if rightMouseButtonTimer.tick() then {
        socket.sendPacket(NetworkPacket.PlayerRightClicked)
        performRightMouseClick()
      }
      if leftMouseButtonTimer.tick() then {
        socket.sendPacket(NetworkPacket.PlayerLeftClicked)
        performLeftMouseClick()
      }

      val worldTickResult = world.tick(Seq(camera), entityEvents)
      worldRenderer.tick(camera, world.renderDistance, worldTickResult)

      if debugOverlay.isDefined then {
        val regularFragmentation = worldRenderer.regularChunkBufferFragmentation
        val transmissiveFragmentation = worldRenderer.transmissiveChunkBufferFragmentation
        val renderQueueLength = worldRenderer.renderQueueLength

        debugOverlay.get.updateContent(
          DebugOverlay.Content.fromCamera(
            camera,
            world.renderDistance,
            regularFragmentation,
            transmissiveFragmentation,
            renderQueueLength
          )
        )
      }

      for s <- overlays do {
        s.tick(ctx)
      }
    } catch {
      case e: ZMQException =>
        println(e)
        logout()
      case e: TimeoutException =>
        println(e)
        logout()
      case e => throw e
    }

    if serverIsShuttingDown then {
      logout()
    }
  }

  private def updateSoundListener(): Unit = {
    val pos = CylCoords(camera.position)
    val at = camera.view.invMatrix.transformDirection(Vector3f(0, 0, -1))
    val up = camera.view.invMatrix.transformDirection(Vector3f(0, 1, 0))

    audioSystem.setListenerPosition(pos.toVector3f)
    audioSystem.setListenerOrientation(at, up)
  }

  private def updatedMousePicker(
      windowSize: WindowSize,
      mouse: MousePosition
  ): Option[MousePickerResult] = {
    if isPaused || isInPopup then {
      return None
    }

    val screenCoords =
      if moveWithMouse then {
        new Vector2f(0, 0)
      } else {
        mouse.normalizedScreenCoords(windowSize.logicalSize)
      }

    // TODO: make it possible to place water on top of a water block (maybe by performing an extra ray trace)
    for
      ray <- Ray.fromScreen(camera, screenCoords)
      hit <- new RayTracer(camera, 7).trace(ray, c => Some(world.getBlock(c)).filter(_.blockType.isSolid))
    yield MousePickerResult(world.getBlock(hit._1), hit._1, hit._2)
  }

  private def performLeftMouseClick(): Unit = {
    selectedBlockAndSide match {
      case Some(MousePickerResult(state, coords, _)) =>
        if state.blockType != Block.Air then {
          world.removeBlock(coords)

          playSoundAt(destroyBlockSoundBuffer, BlockCoords(coords).toCylCoords)
        }
      case _ =>
    }
  }

  private def performRightMouseClick(): Unit = {
    selectedBlockAndSide match {
      case Some(MousePickerResult(state, coords, Some(side))) =>
        val coordsInFront = coords.offset(NeighborOffsets(side))

        state.blockType match {
          case Block.Tnt => explode(coords)
          case _         => tryPlacingBlockAt(coordsInFront, player)
        }
      case _ =>
    }
  }

  private def tryPlacingBlockAt(coords: BlockRelWorld, player: Player): Unit = {
    if world.getBlock(coords).blockType.isSolid then {
      return
    }

    val blockType = player.blockInHand
    if blockType == Block.Air then {
      return
    }

    val state = new BlockState(blockType)

    val collides = world.collisionDetector.collides(
      blockType.bounds(state.metadata),
      BlockCoords(coords).toCylCoords,
      player.bounds,
      CylCoords(camera.position)
    )

    if !collides then {
      world.setBlock(coords, state)

      playSoundAt(placeBlockSoundBuffer, BlockCoords(coords).toCylCoords)
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
    Await.ready(logout().andThen(_ => socket.close()), 100.millis)

    setMouseCursorInvisible(false)

    for s <- overlays do {
      s.unload()
    }

    world.unload()
    worldRenderer.unload()
    crosshairVAO.free()
    crosshairShader.free()
    toolbar.unload()
    blockInHandRenderer.unload()

    if debugOverlay.isDefined then {
      debugOverlay.get.unload()
    }

    executorService.shutdown()
  }
}

case class MousePickerResult(block: BlockState, coords: BlockRelWorld, side: Option[Int])

class GameClientSocket(serverIp: String, serverPort: Int) {
  private val context = ZContext()
  private val socket = context.createSocket(SocketType.DEALER)

  private val clientId = (new Random().nextInt(1000000) + 1000000).toString

  socket.setIdentity(clientId.getBytes)
  socket.setSendTimeOut(3000)
  socket.setReceiveTimeOut(3000)
  socket.setReconnectIVL(-1)
  socket.setHeartbeatIvl(200)
  socket.setHeartbeatTimeout(1000)
  socket.connect(s"tcp://$serverIp:$serverPort")

  def sendPacket(packet: NetworkPacket): Unit = this.synchronized {
    val message = packet.serialize()

    if !socket.send(message) then {
      val err = socket.errno()
      throw new ZMQException("Could not send message", err)
    }
  }

  def sendPacketAndWait(packet: NetworkPacket): Nbt = this.synchronized {
    if !socket.send(packet.serialize()) then {
      val err = socket.errno()
      throw new ZMQException("Could not send message", err)
    }

    val response = socket.recv(0)
    if response == null then {
      val err = socket.errno()
      throw new ZMQException("Could not receive message", err)
    }

    val (_, tag) = Nbt.fromBinary(response)
    tag
  }

  def sendMultiplePacketsAndWait(packets: Seq[NetworkPacket]): Seq[Nbt] = this.synchronized {
    for p <- packets do {
      if !socket.send(p.serialize()) then {
        val err = socket.errno()
        throw new ZMQException("Could not send message", err)
      }
    }

    for i <- packets.indices yield {
      val response = socket.recv(0)
      if response == null then {
        val err = socket.errno()
        throw new ZMQException(s"Could not receive message ${i + 1}", err)
      }

      val (_, tag) = Nbt.fromBinary(response)
      tag
    }
  }

  def close(): Unit = {
    context.close()
  }
}
