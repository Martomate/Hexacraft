package hexacraft.client

import hexacraft.game.*
import hexacraft.gui.{Event, LocationInfo, MousePosition, RenderContext, TickContext, WindowSize}
import hexacraft.gui.comp.{Component, GUITransformation}
import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.window.{KeyAction, KeyboardKey, MouseAction, MouseButton}
import hexacraft.renderer.{Renderer, TextureArray, VAO}
import hexacraft.shaders.CrosshairShader
import hexacraft.util.{Channel, TickableTimer}
import hexacraft.world.*
import hexacraft.world.block.{Block, BlockSpec, BlockState}
import hexacraft.world.chunk.{Chunk, ChunkColumnData, ChunkColumnHeightMap, ChunkColumnTerrain}
import hexacraft.world.coord.{BlockCoords, BlockRelWorld, CoordUtils, CylCoords, NeighborOffsets}

import com.martomate.nbt.Nbt
import org.joml.{Matrix4f, Vector2f, Vector3d, Vector3f}
import org.zeromq.*
import zmq.ZError

import java.util.UUID
import scala.collection.mutable
import scala.util.Random

object GameClient {
  enum Event {
    case GameQuit
    case CursorCaptured
    case CursorReleased
  }

  def create(
      serverIp: String,
      serverPort: Int,
      isOnline: Boolean,
      keyboard: GameKeyboard,
      blockLoader: BlockTextureLoader,
      initialWindowSize: WindowSize,
      audioSystem: AudioSystem
  ): (GameClient, Channel.Receiver[GameClient.Event]) = {
    val socket = GameClientSocket(serverIp, serverPort)

    val worldInfo = WorldInfo.fromNBT(
      socket.sendPacketAndWait(NetworkPacket.GetWorldInfo).asInstanceOf[Nbt.MapTag],
      null,
      WorldSettings.none
    )

    val playerNbt = socket.sendPacketAndWait(NetworkPacket.GetPlayerState)
    val player = Player.fromNBT(playerNbt.asInstanceOf[Nbt.MapTag])

    val blockSpecs = BlockSpecs.default
    val blockTextureMapping = loadBlockTextures(blockSpecs, blockLoader)
    val blockTextureIndices: Map[String, IndexedSeq[Int]] =
      blockSpecs.view.mapValues(spec => spec.textures.indices(blockTextureMapping.texIdxMap)).toMap

    TextureArray.registerTextureArray("blocks", 32, blockTextureMapping.images)

    val crosshairShader = new CrosshairShader()
    val crosshairVAO: VAO = CrosshairShader.createVao()
    val crosshairRenderer: Renderer = CrosshairShader.createRenderer()

    // TODO: get chunks from the server instead of generating the entire world
    val world = ClientWorld(worldInfo)

    given CylinderSize = world.size

    val worldRenderer: WorldRenderer = new WorldRenderer(world, blockTextureIndices, initialWindowSize.physicalSize)

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

    Thread(() => socket.runMonitoring()).start()

    (client, rx)
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

  private def loadBlockTextures(blockSpecs: Map[String, BlockSpec], blockLoader: BlockTextureLoader) = {
    val textures = blockSpecs.values.map(_.textures)
    val squareTextureNames = textures.flatMap(_.sides).toSet.toSeq.map(name => s"$name.png")
    val triTextureNames = (textures.map(_.top) ++ textures.map(_.bottom)).toSet.toSeq.map(name => s"$name.png")
    blockLoader.load(squareTextureNames, triTextureNames)
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
  private var moveWithMouse: Boolean = false
  private var isPaused: Boolean = false
  private var isInPopup: Boolean = false

  private var debugOverlay: Option[DebugOverlay] = None

  private var selectedBlockAndSide: Option[(BlockState, BlockRelWorld, Option[Int])] = None
  private val overlays: mutable.ArrayBuffer[Component] = mutable.ArrayBuffer.empty

  private val rightMouseButtonTimer: TickableTimer = TickableTimer(10, initEnabled = false)
  private val leftMouseButtonTimer: TickableTimer = TickableTimer(10, initEnabled = false)
  private val walkSoundTimer: TickableTimer = TickableTimer(20, initEnabled = false)

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
        eventHandler.send(GameClient.Event.GameQuit)
    }

    overlays += pauseMenu
    setPaused(true)
  }

  private def handleKeyPress(key: KeyboardKey): Unit = key match {
    case KeyboardKey.Escape =>
      pauseGame()
    case KeyboardKey.Letter('E') =>
      import InventoryBox.Event.*

      if !isPaused then {
        val (tx, rx) = Channel[InventoryBox.Event]()
        val inventoryScene = InventoryBox(player.inventory, blockTextureIndices)(tx)

        rx.onEvent {
          case BoxClosed =>
            overlays -= inventoryScene
            inventoryScene.unload()
            isInPopup = false
            setUseMouse(true)
          case InventoryUpdated(inv) =>
            val invNbt = socket.sendPacketAndWait(NetworkPacket.PlayerUpdatedInventory(inv))
            val serverInv = Inventory.fromNBT(invNbt.asMap.get)
            player.inventory = serverInv
            toolbar.onInventoryUpdated(serverInv)
        }

        overlays += inventoryScene
        isInPopup = true
        setUseMouse(false)
      }
    case KeyboardKey.Letter('M') =>
      setUseMouse(!moveWithMouse)
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
      if !isPaused then {
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

  def render(transformation: GUITransformation)(using RenderContext): Unit = {
    worldRenderer.render(camera, new Vector3f(0, 1, -1), selectedBlockAndSide)

    renderCrosshair()

    blockInHandRenderer.render(transformation)
    toolbar.render(transformation)

    if debugOverlay.isDefined then {
      debugOverlay.get.render(transformation)
    }

    for s <- overlays do {
      s.render(transformation)
    }
  }

  private def renderCrosshair(): Unit = {
    if isPaused || isInPopup || !moveWithMouse then {
      return
    }

    crosshairShader.enable()
    crosshairRenderer.render(crosshairVAO, crosshairVAO.maxCount)
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

  private val prio = ChunkLoadingPrioritizer(world.renderDistance)

  def tick(ctx: TickContext): Unit = {
    try {
      if socket.isDisconnected then {
        eventHandler.send(GameClient.Event.GameQuit)
        return
      }

      val playerNbt = socket.sendPacketAndWait(NetworkPacket.GetPlayerState)
      val syncedPlayer = Player.fromNBT(playerNbt.asInstanceOf[Nbt.MapTag])
      // println(syncedPlayer.position)
      if player.position.sub(syncedPlayer.position, new Vector3d).length() > 10.0 then {}
      player.position.add(syncedPlayer.position.sub(player.position, new Vector3d).mul(0.1))
      val rotationDiff = syncedPlayer.rotation.sub(player.rotation, new Vector3d)
      if rotationDiff.y < -math.Pi then {
        rotationDiff.y += math.Pi * 2
      }
      if rotationDiff.y > math.Pi then {
        rotationDiff.y -= math.Pi * 2
      }
      //    player.rotation.add(rotationDiff.mul(0.1))
      player.flying = syncedPlayer.flying

      val worldEventsNbtPacket = socket.sendPacketAndWait(NetworkPacket.GetEvents).asMap.get

      val blockUpdatesNbtList = worldEventsNbtPacket.getList("block_updates").getOrElse(Seq()).map(_.asMap.get)
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

      val entityEventsNbt = worldEventsNbtPacket.getMap("entity_events").get
      val entityEventIds = entityEventsNbt.getList("ids").get.map(_.asInstanceOf[Nbt.StringTag].v)
      val entityEventData = entityEventsNbt.getList("events").get.map(_.asMap.get)
      val entityEvents = for (id, eventNbt) <- entityEventIds.zip(entityEventData) yield {
        val event = eventNbt.getString("type").get match {
          case "spawned"   => EntityEvent.Spawned(eventNbt.getMap("data").get)
          case "despawned" => EntityEvent.Despawned
          case "position"  => EntityEvent.Position(CylCoords(eventNbt.getMap("pos").get.setVector(new Vector3d)))
          case "rotation"  => EntityEvent.Rotation(eventNbt.getMap("r").get.setVector(new Vector3d))
          case "velocity"  => EntityEvent.Velocity(eventNbt.getMap("v").get.setVector(new Vector3d))
          case "flying"    => EntityEvent.Flying(eventNbt.getBoolean("f", false))
        }

        (UUID.fromString(id), event)
      }

      updateSoundListener()

      prio.tick(PosAndDir.fromCameraView(camera.view))
      prio.nextAddableChunk match {
        case Some(chunkCoords) =>
          var success = true
          val columnCoords = chunkCoords.getColumnRelWorld
          if world.getColumn(columnCoords).isEmpty then {
            val columnNbt = socket.sendPacketAndWait(NetworkPacket.LoadColumnData(columnCoords))
            if columnNbt != Nbt.emptyMap then {
              val column = ChunkColumnTerrain.create(
                ChunkColumnHeightMap.fromData2D(world.worldGenerator.getHeightmapInterpolator(columnCoords)),
                Some(ChunkColumnData.fromNbt(columnNbt.asInstanceOf[Nbt.MapTag]))
              )
              world.setColumn(columnCoords, column)
            } else {
              success = false
            }
          }
          if success && world.getChunk(chunkCoords).isEmpty then {
            val chunkNbt = socket.sendPacketAndWait(NetworkPacket.LoadChunkData(chunkCoords))
            if chunkNbt != Nbt.emptyMap then {
              val chunk = Chunk.fromNbt(chunkNbt.asInstanceOf[Nbt.MapTag])
              world.setChunk(chunkCoords, chunk)
            } else {
              success = false
            }
          }
          if success then {
            prio += chunkCoords
          }
        case None =>
      }

      val playerCoords = CoordUtils.approximateIntCoords(CylCoords(player.position).toBlockCoords)

      if world.getChunk(playerCoords.getChunkRelWorld).isDefined then {
        val pressedKeys = keyboard.pressedKeys
        val maxSpeed = playerInputHandler.determineMaxSpeed(pressedKeys)
        if !isPaused && !isInPopup then {
          val mouseMovement = if moveWithMouse then ctx.mouseMovement else new Vector2f
          val isInFluid = playerEffectiveViscosity(player) > Block.Air.viscosity.toSI * 2

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
            playerEffectiveViscosity(player),
            playerVolumeSubmergedInWater(player)
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

        val sourceId = audioSystem.createSoundSource(sound)
        audioSystem.setSoundSourcePosition(sourceId, CylCoords(player.position).toVector3f)
        audioSystem.startPlayingSound(sourceId)
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

        debugOverlay.get.updateContent(
          DebugOverlay.Content.fromCamera(camera, world.renderDistance, regularFragmentation, transmissiveFragmentation)
        )
      }

      for s <- overlays do {
        s.tick(ctx)
      }
    } catch {
      case e: ZMQException =>
        println(e)
        eventHandler.send(GameClient.Event.GameQuit)
      case e => throw e
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
  ): Option[(BlockState, BlockRelWorld, Option[Int])] = {
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
    yield (world.getBlock(hit._1), hit._1, hit._2)
  }

  def performLeftMouseClick(): Unit = {
    val blockAndSide = selectedBlockAndSide

    blockAndSide match {
      case Some((state, coords, _)) =>
        if state.blockType != Block.Air then {
          world.removeBlock(coords)

          val sourceId = audioSystem.createSoundSource(destroyBlockSoundBuffer)
          audioSystem.setSoundSourcePosition(sourceId, BlockCoords(coords).toCylCoords.toVector3f)
          audioSystem.startPlayingSound(sourceId)
        }
      case _ =>
    }
  }

  def performRightMouseClick(): Unit = {
    val blockAndSide = selectedBlockAndSide

    blockAndSide match {
      case Some((state, coords, Some(side))) =>
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

      val sourceId = audioSystem.createSoundSource(placeBlockSoundBuffer)
      audioSystem.setSoundSourcePosition(sourceId, BlockCoords(coords).toCylCoords.toVector3f)
      audioSystem.startPlayingSound(sourceId)
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
    socket.close()

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
  }
}

class GameClientSocket(serverIp: String, serverPort: Int) {
  private val context = ZContext()
  context.setUncaughtExceptionHandler((thread, exc) => println(s"Uncaught exception: $exc"))
  context.setNotificationExceptionHandler((thread, exc) => println(s"Notification: $exc"))

  private val socket = context.createSocket(SocketType.DEALER)

  private val clientId = (new Random().nextInt(1000000) + 1000000).toString

  socket.setIdentity(clientId.getBytes)
  socket.setSendTimeOut(3000)
  socket.setReceiveTimeOut(3000)
  socket.setReconnectIVL(-1)
  socket.setHeartbeatIvl(200)
  socket.setHeartbeatTimeout(1000)
  socket.connect(s"tcp://$serverIp:$serverPort")

  private var monitoringThread: Thread = null.asInstanceOf[Thread]

  private var _disconnected: Boolean = false
  def isDisconnected: Boolean = _disconnected

  def runMonitoring(): Unit = {
    if monitoringThread != null then {
      throw new Exception("May only run monitoring once")
    }
    monitoringThread = Thread.currentThread()

    val monitor = ZMonitor(context, socket)
    monitor.add(ZMonitor.Event.ALL)
    monitor.verbose(false)
    monitor.start()

    while !context.isClosed do {
      try {
        val event = monitor.nextEvent(100)
        if event != null then {
          if event.`type` == ZMonitor.Event.DISCONNECTED then {
            _disconnected = true
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
    }

    monitor.close()
  }

  def sendPacket(packet: NetworkPacket): Unit = this.synchronized {
    val message = packet.serialize()

    if !socket.send(message) then {
      val err = socket.errno()
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

  def sendPacketAndWait(packet: NetworkPacket): Nbt = {
    val response = queryRaw(packet.serialize())
    val (_, tag) = Nbt.fromBinary(response)
    tag
  }

  def close(): Unit = {
    context.close()
  }
}
