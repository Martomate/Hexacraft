package hexacraft.game

import hexacraft.game
import hexacraft.gui.{Event, LocationInfo, MousePosition, RenderContext, TickContext, WindowSize}
import hexacraft.gui.comp.{Component, GUITransformation}
import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.window.{KeyAction, KeyboardKey, MouseAction, MouseButton}
import hexacraft.renderer.{Renderer, TextureArray, VAO}
import hexacraft.shaders.CrosshairShader
import hexacraft.util.{Channel, TickableTimer}
import hexacraft.world.{
  Camera,
  CameraProjection,
  CylinderSize,
  HexBox,
  Player,
  World,
  WorldInfo,
  WorldProvider,
  WorldSettings
}
import hexacraft.world.block.{Block, BlockSpec, BlockState}
import hexacraft.world.block.BlockSpec.{Offsets, Textures}
import hexacraft.world.coord.{BlockCoords, BlockRelWorld, CoordUtils, CylCoords, NeighborOffsets}
import hexacraft.world.entity.{
  BoundsComponent,
  Entity,
  EntityFactory,
  ModelComponent,
  PlayerEntityModel,
  TransformComponent,
  VelocityComponent
}
import hexacraft.world.render.WorldRenderer

import com.martomate.nbt.Nbt
import org.joml.{Matrix4f, Vector2f, Vector3f}
import org.zeromq.*
import zmq.ZError

import scala.collection.mutable
import scala.util.Random

class EmptyWorldProvider(worldInfo: WorldInfo) extends WorldProvider {
  override def getWorldInfo: WorldInfo = worldInfo

  override def loadState(path: String): Option[Nbt.MapTag] = None

  override def saveState(tag: Nbt.MapTag, name: String, path: String): Unit = ()
}

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

    val blockSpecs = makeBlockSpecs()
    val blockTextureMapping = loadBlockTextures(blockSpecs, blockLoader)
    val blockTextureIndices: Map[String, IndexedSeq[Int]] =
      blockSpecs.view.mapValues(spec => spec.textures.indices(blockTextureMapping.texIdxMap)).toMap

    TextureArray.registerTextureArray("blocks", 32, blockTextureMapping.images)

    val crosshairShader = new CrosshairShader()
    val crosshairVAO: VAO = CrosshairShader.createVao()
    val crosshairRenderer: Renderer = CrosshairShader.createRenderer()

    // TODO: get chunks from the server instead of generating the entire world
    val world = World(new EmptyWorldProvider(worldInfo), worldInfo)

    given CylinderSize = world.size

    val player: Player = makePlayer(worldInfo.player, world)
    val otherPlayer: Player = makePlayer(worldInfo.player, world)
    val otherPlayerEntity: Entity = makeOtherPlayer(player)

    val worldRenderer: WorldRenderer = new WorldRenderer(world, blockTextureIndices, initialWindowSize.physicalSize)

    if isOnline then {
      worldRenderer.addPlayer(otherPlayerEntity)
    }

    val camera: Camera = new Camera(makeCameraProjection(initialWindowSize, world.size.worldSize))

    val playerInputHandler: PlayerInputHandler = new PlayerInputHandler
    val playerPhysicsHandler: PlayerPhysicsHandler = new PlayerPhysicsHandler(world, world.collisionDetector)

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
      otherPlayer,
      otherPlayerEntity,
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
      world: World,
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

  private def makePlayer(playerNbt: Nbt.MapTag, world: World): Player = {
    given CylinderSize = world.size

    if playerNbt != null then {
      Player.fromNBT(playerNbt)
    } else {
      val startX = (math.random() * 100 - 50).toInt
      val startZ = (math.random() * 100 - 50).toInt
      val startY = world.getHeight(startX, startZ) + 4
      Player.atStartPos(BlockCoords(startX, startY, startZ).toCylCoords)
    }
  }

  private def makeBlockSpecs() = Map(
    "stone" -> BlockSpec(Textures.basic("stoneSide").withTop("stoneTop").withBottom("stoneTop")),
    "grass" -> BlockSpec(Textures.basic("grassSide").withTop("grassTop").withBottom("dirt")),
    "dirt" -> BlockSpec(Textures.basic("dirt")),
    "sand" -> BlockSpec(Textures.basic("sand")),
    "water" -> BlockSpec(Textures.basic("water")),
    "log" -> BlockSpec(
      Textures
        .basic("logSide")
        .withTop("log", Offsets(0, 1, 2, 0, 1, 2))
        .withBottom("log", Offsets(0, 1, 2, 0, 1, 2))
    ),
    "leaves" -> BlockSpec(Textures.basic("leaves")),
    "planks" -> BlockSpec(
      Textures
        .basic("planks_side")
        .withTop("planks_top", Offsets(0, 1, 0, 1, 0, 1))
        .withBottom("planks_top", Offsets(0, 1, 0, 1, 0, 1))
    ),
    "log_birch" -> BlockSpec(
      Textures
        .basic("logSide_birch")
        .withTop("log_birch", Offsets(0, 1, 2, 0, 1, 2))
        .withBottom("log_birch", Offsets(0, 1, 2, 0, 1, 2))
    ),
    "leaves_birch" -> BlockSpec(Textures.basic("leaves_birch")),
    "tnt" -> BlockSpec(Textures.basic("tnt").withTop("tnt_top").withBottom("tnt_top"))
  )

  private def loadBlockTextures(blockSpecs: Map[String, BlockSpec], blockLoader: BlockTextureLoader) = {
    val textures = blockSpecs.values.map(_.textures)
    val squareTextureNames = textures.flatMap(_.sides).toSet.toSeq.map(name => s"$name.png")
    val triTextureNames = (textures.map(_.top) ++ textures.map(_.bottom)).toSet.toSeq.map(name => s"$name.png")
    blockLoader.load(squareTextureNames, triTextureNames)
  }

  private def makeOtherPlayer(player: Player)(using CylinderSize) = {
    Entity(
      null,
      Seq(
        TransformComponent(CylCoords(player.position).offset(-2, -2, -1)),
        VelocityComponent(),
        BoundsComponent(EntityFactory.playerBounds),
        ModelComponent(PlayerEntityModel.create("player"))
      )
    )
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
    world: World,
    val player: Player,
    val otherPlayer: Player,
    otherPlayerEntity: Entity,
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
    case KeyboardKey.Letter('B') =>
      val newCoords = camera.blockCoords.offset(0, -4, 0)

      if world.getBlock(newCoords).blockType == Block.Air then {
        world.setBlock(newCoords, new BlockState(player.blockInHand))
      }
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
            player.inventory = inv
            toolbar.onInventoryUpdated(inv)
        }

        overlays += inventoryScene
        isInPopup = true
        setUseMouse(false)
      }
    case KeyboardKey.Letter('M') =>
      setUseMouse(!moveWithMouse)
    case KeyboardKey.Letter('F') =>
      player.flying = !player.flying
    case KeyboardKey.Function(7) =>
      setDebugScreenVisible(debugOverlay.isEmpty)
    case KeyboardKey.Digit(digit) =>
      if digit > 0 then {
        setSelectedItemSlot(digit - 1)
      }
    case KeyboardKey.Letter('P') =>
      world.addEntity(EntityFactory.atStartPos(CylCoords(player.position), "player").unwrap())
    case KeyboardKey.Letter('L') =>
      world.addEntity(EntityFactory.atStartPos(CylCoords(player.position), "sheep").unwrap())
    case KeyboardKey.Letter('K') =>
      world.removeAllEntities()
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
            setSelectedItemSlot((player.selectedItemSlot + dy + 9) % 9)
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

  def tick(ctx: TickContext): Unit = {
    if socket.isDisconnected then {
      eventHandler.send(GameClient.Event.GameQuit)
      return
    }

    updateSoundListener()

    try {
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
        playerPhysicsHandler.tick(
          otherPlayer,
          maxSpeed,
          playerEffectiveViscosity(otherPlayer),
          playerVolumeSubmergedInWater(otherPlayer)
        )

        if !isPaused then {
          walkSoundTimer.enabled = !player.flying && player.velocity.y == 0 && player.velocity.lengthSquared() > 0.01
        }
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
        performRightMouseClick(true)
      }
      if leftMouseButtonTimer.tick() then {
        socket.sendPacket(NetworkPacket.PlayerLeftClicked)
        performLeftMouseClick(true)
      }

      otherPlayerEntity.transform.position = CylCoords(otherPlayer.position)
        .offset(0, otherPlayer.bounds.bottom.toDouble, 0)
      otherPlayerEntity.transform.rotation.set(0, math.Pi * 0.5 - otherPlayer.rotation.y, 0)
      otherPlayerEntity.velocity.velocity.set(otherPlayer.velocity)

      otherPlayerEntity.model.foreach(_.tick(otherPlayerEntity.velocity.velocity.lengthSquared() > 0.1))

      val worldTickResult = world.tick(Seq(camera))
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
      case e: ZMQException => println(e)
      case e               => throw e
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

  def performLeftMouseClick(localPlayer: Boolean): Unit = {
    val blockAndSide =
      if localPlayer then selectedBlockAndSide
      else {
        val otherCamera = Camera(camera.proj)
        otherCamera.setPositionAndRotation(otherPlayer.position, otherPlayer.rotation)
        otherCamera.updateCoords()
        otherCamera.updateViewMatrix(camera.view.position)
        for
          ray <- Ray.fromScreen(otherCamera, Vector2f(0, 0))
          hit <- new RayTracer(otherCamera, 7).trace(ray, c => Some(world.getBlock(c)).filter(_.blockType.isSolid))
        yield (world.getBlock(hit._1), hit._1, hit._2)
      }

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

  def performRightMouseClick(localPlayer: Boolean): Unit = {
    val blockAndSide =
      if localPlayer then selectedBlockAndSide
      else {
        val otherCamera = Camera(camera.proj)
        otherCamera.setPositionAndRotation(otherPlayer.position, otherPlayer.rotation)
        otherCamera.updateCoords()
        otherCamera.updateViewMatrix(camera.view.position)
        for
          ray <- Ray.fromScreen(otherCamera, Vector2f(0, 0))
          hit <- new RayTracer(otherCamera, 7).trace(ray, c => Some(world.getBlock(c)).filter(_.blockType.isSolid))
        yield (world.getBlock(hit._1), hit._1, hit._2)
      }

    blockAndSide match {
      case Some((state, coords, Some(side))) =>
        val coordsInFront = coords.offset(NeighborOffsets(side))

        state.blockType match {
          case Block.Tnt => explode(coords)
          case _         => tryPlacingBlockAt(coordsInFront, if localPlayer then player else otherPlayer)
        }
      case _ =>
    }
  }

  private def tryPlacingBlockAt(coords: BlockRelWorld, player: Player): Unit = {
    if world.getBlock(coords).blockType.isSolid then {
      return
    }

    val blockType = player.blockInHand
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
  private val clientId = (new Random().nextInt(1000000) + 1000000).toString

  private val context = ZContext()
  context.setUncaughtExceptionHandler((thread, exc) => println(s"Uncaught exception: $exc"))
  context.setNotificationExceptionHandler((thread, exc) => println(s"Notification: $exc"))

  private val socket = context.createSocket(SocketType.DEALER)

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
    try {
      while !Thread.interrupted() do {
        val event = monitor.nextEvent(100)
        if event != null then {
          if event.`type` == ZMonitor.Event.DISCONNECTED then {
            _disconnected = true
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

  def sendPacket(packet: NetworkPacket): Unit = this.synchronized {
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

  def sendPacketAndWait(packet: NetworkPacket): Nbt = {
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
