package hexacraft.game

import hexacraft.game.inventory.{GuiBlockRenderer, InventoryBox, Toolbar}
import hexacraft.gui.*
import hexacraft.gui.comp.{Component, GUITransformation}
import hexacraft.infra.fs.BlockTextureLoader
import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.window.*
import hexacraft.renderer.*
import hexacraft.util.{ResourceWrapper, TickableTimer, Tracker}
import hexacraft.world.{World, WorldProvider}
import hexacraft.world.block.{Block, BlockSpecRegistry, BlockState, HexBox}
import hexacraft.world.camera.{Camera, CameraProjection}
import hexacraft.world.coord.CoordUtils
import hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import hexacraft.world.coord.integer.{BlockRelWorld, NeighborOffsets}
import hexacraft.world.entity.{EntityBaseData, EntityModel, EntityModelLoader}
import hexacraft.world.entity.player.ControlledPlayerEntity
import hexacraft.world.player.Player
import hexacraft.world.ray.{Ray, RayTracer}
import hexacraft.world.render.WorldRenderer
import hexacraft.world.settings.WorldInfo

import com.flowpowered.nbt.CompoundTag
import org.joml.{Matrix4f, Vector2f, Vector3f}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object GameScene {
  enum Event:
    case QuitGame
}

class GameScene(worldProvider: WorldProvider)(eventHandler: Tracker[GameScene.Event])(using
    mouse: GameMouse,
    keyboard: GameKeyboard,
    window: GameWindow,
    blockLoader: BlockTextureLoader
)(using WindowExtras)
    extends Scene:

  TextureArray.registerTextureArray("blocks", 32, new ResourceWrapper(() => blockLoader.reload().images))

  given BlockSpecRegistry = BlockSpecRegistry.load(blockLoader.textureMapping)

  private val crosshairShader = new CrosshairShader()
  private val crosshairVAO: VAO = makeCrosshairVAO
  private val crosshairRenderer: Renderer =
    new Renderer(OpenGL.PrimitiveMode.Lines, GpuState.of(OpenGL.State.DepthTest -> false))

  given entityModelLoader: EntityModelLoader = new EntityModelLoader()
  private val playerModel: EntityModel = entityModelLoader.load("player")
  private val sheepModel: EntityModel = entityModelLoader.load("sheep")

  private val worldInfo = worldProvider.getWorldInfo
  private val world = World(worldProvider, worldInfo)
  import world.size.impl

  val player: Player = makePlayer(worldInfo.player)

  private val overlays = mutable.ArrayBuffer.empty[Component]

  private val otherPlayer: ControlledPlayerEntity =
    new ControlledPlayerEntity(playerModel, new EntityBaseData(CylCoords(player.position)))

  private val worldRenderer: WorldRenderer = new WorldRenderer(world, window.framebufferSize)
  world.trackEvents(worldRenderer.onWorldEvent _)

  // worldRenderer.addPlayer(otherPlayer)
  otherPlayer.setPosition(otherPlayer.position.offset(-2, -2, -1))

  val camera: Camera = new Camera(makeCameraProjection)

  private val mousePicker: RayTracer = new RayTracer(world, camera, 7)
  private val playerInputHandler: PlayerInputHandler = new PlayerInputHandler(keyboard, player)
  private val playerPhysicsHandler: PlayerPhysicsHandler =
    new PlayerPhysicsHandler(player, world, world.collisionDetector)

  private var selectedBlockAndSide: Option[(BlockState, BlockRelWorld, Option[Int])] = None

  private val toolbar: Toolbar = makeToolbar(player)
  private val blockInHandRenderer: GuiBlockRenderer = makeBlockInHandRenderer(world, camera)
  updateBlockInHandRendererContent()

  private val rightMouseButtonTimer: TickableTimer = TickableTimer(10, initEnabled = false)
  private val leftMouseButtonTimer: TickableTimer = TickableTimer(10, initEnabled = false)

  private var moveWithMouse: Boolean = false
  private var isPaused: Boolean = false
  private var isInPopup: Boolean = false

  private var debugOverlay: DebugOverlay = _
  private var pauseMenu: PauseMenu = _
  private var inventoryScene: InventoryBox = _

  setUniforms()
  setUseMouse(true)

  saveWorldInfo()

  private def saveWorldInfo(): Unit =
    val worldTag = new WorldInfo(worldInfo.worldName, worldInfo.worldSize, worldInfo.gen, player.toNBT).toNBT
    worldProvider.saveWorldData(worldTag)

  override def onReloadedResources(): Unit =
    for s <- overlays do s.onReloadedResources()
    setUniforms()
    world.onReloadedResources()

  private def setUniforms(): Unit =
    setProjMatrixForAll()
    worldRenderer.onTotalSizeChanged(world.size.totalSize)
    crosshairShader.setWindowAspectRatio(window.aspectRatio)

  private def setProjMatrixForAll(): Unit =
    worldRenderer.onProjMatrixChanged(camera)

  private def handleKeyPress(key: KeyboardKey): Unit = key match
    case KeyboardKey.Letter('B') =>
      val newCoords = camera.blockCoords.offset(0, -4, 0)

      if world.getBlock(newCoords).blockType == Block.Air
      then world.setBlock(newCoords, new BlockState(player.blockInHand))
    case KeyboardKey.Escape =>
      import PauseMenu.Event.*

      pauseMenu = PauseMenu:
        case Unpause =>
          overlays -= pauseMenu
          pauseMenu.unload()
          pauseMenu = null
          setPaused(false)
        case QuitGame => eventHandler.notify(GameScene.Event.QuitGame)

      overlays += pauseMenu
      setPaused(true)
    case KeyboardKey.Letter('E') =>
      if !isPaused
      then
        setUseMouse(false)
        isInPopup = true
        inventoryScene = InventoryBox(player.inventory)(() => {
          overlays -= inventoryScene
          inventoryScene.unload()
          inventoryScene = null
          isInPopup = false
          setUseMouse(true)
        })

        overlays += inventoryScene
    case KeyboardKey.Letter('M') =>
      setUseMouse(!moveWithMouse)
    case KeyboardKey.Letter('F') =>
      player.flying = !player.flying
    case KeyboardKey.Function(7) =>
      setDebugScreenVisible(debugOverlay == null)
    case KeyboardKey.Digit(digit) =>
      if digit > 0 then setSelectedItemSlot(digit - 1)
    case KeyboardKey.Letter('P') =>
      val startPos = CylCoords(player.position)

      world.addEntity(world.entityRegistry.get("player").get.atStartPos(startPos))
    case KeyboardKey.Letter('L') =>
      val startPos = CylCoords(player.position)

      world.addEntity(world.entityRegistry.get("sheep").get.atStartPos(startPos))
    case KeyboardKey.Letter('K') =>
      world.removeAllEntities()
    case _ =>

  private def setDebugScreenVisible(visible: Boolean): Unit =
    if visible
    then
      if debugOverlay == null
      then debugOverlay = new DebugOverlay
    else
      if debugOverlay != null
      then debugOverlay.unload()
      debugOverlay = null

  private def setUseMouse(useMouse: Boolean): Unit =
    moveWithMouse = useMouse
    setMouseCursorInvisible(moveWithMouse)
    summon[WindowExtras].resetMousePos()

  override def handleEvent(event: Event): Boolean =
    if overlays.reverseIterator.exists(_.handleEvent(event))
    then true
    else
      import Event.*
      event match
        case KeyEvent(key, _, action, _) =>
          if action == KeyAction.Press then handleKeyPress(key)
        case ScrollEvent(_, yOffset, _) if !isPaused && !isInPopup && moveWithMouse =>
          val dy = -math.signum(yOffset).toInt
          if dy != 0 then setSelectedItemSlot((player.selectedItemSlot + dy + 9) % 9)
        case MouseClickEvent(button, action, _, _) =>
          button match
            case MouseButton.Left  => leftMouseButtonTimer.enabled = action != MouseAction.Release
            case MouseButton.Right => rightMouseButtonTimer.enabled = action != MouseAction.Release
            case _                 =>
        case _ =>
      true

  private def setSelectedItemSlot(itemSlot: Int): Unit =
    player.selectedItemSlot = itemSlot
    updateBlockInHandRendererContent()
    toolbar.setSelectedIndex(itemSlot)

  private def updateBlockInHandRendererContent(): Unit =
    blockInHandRenderer.updateContent(1.5f, -0.9f, Seq(player.blockInHand))

  private def setPaused(paused: Boolean): Unit =
    if isPaused != paused
    then
      isPaused = paused
      setMouseCursorInvisible(!paused && moveWithMouse)

  private def setMouseCursorInvisible(invisible: Boolean): Unit =
    summon[WindowExtras].setCursorMode(if invisible then CursorMode.Disabled else CursorMode.Normal)

  override def windowResized(width: Int, height: Int): Unit =
    val aspectRatio = width.toFloat / height
    camera.proj.aspect = aspectRatio
    camera.updateProjMatrix()

    setProjMatrixForAll()
    blockInHandRenderer.setWindowAspectRatio(aspectRatio)
    toolbar.setWindowAspectRatio(aspectRatio)

    crosshairShader.setWindowAspectRatio(aspectRatio)

  override def framebufferResized(width: Int, height: Int): Unit =
    worldRenderer.framebufferResized(width, height)

  override def render(transformation: GUITransformation)(using RenderContext): Unit =
    worldRenderer.render(camera, new Vector3f(0, 1, -1), selectedBlockAndSide)

    renderCrosshair()

    blockInHandRenderer.render(transformation)
    toolbar.render(transformation)

    if debugOverlay != null
    then debugOverlay.render(transformation)

    for s <- overlays do s.render(transformation)

  private def renderCrosshair(): Unit =
    if !isPaused && !isInPopup && moveWithMouse
    then
      crosshairShader.enable()
      crosshairRenderer.render(crosshairVAO)

  private def playerEffectiveViscosity: Double =
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

  private def playerVolumeSubmergedInWater: Double =
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

  override def tick(): Unit =
    val playerCoords = CoordUtils.approximateIntCoords(CylCoords(player.position).toBlockCoords)
    if world.getChunk(playerCoords.getChunkRelWorld).isDefined
    then
      val maxSpeed = playerInputHandler.maxSpeed
      if !isPaused && !isInPopup then
        playerInputHandler.tick(
          if moveWithMouse then mouse.moved else new Vector2f,
          maxSpeed,
          playerEffectiveViscosity > Block.Air.viscosity.toSI * 2
        )
      if !isPaused then playerPhysicsHandler.tick(maxSpeed, playerEffectiveViscosity, playerVolumeSubmergedInWater)

    camera.setPositionAndRotation(player.position, player.rotation)
    camera.updateCoords()
    camera.updateViewMatrix()

    updateBlockInHandRendererContent()

    selectedBlockAndSide = updatedMousePicker()

    if (rightMouseButtonTimer.tick()) {
      performRightMouseClick()
    }
    if (leftMouseButtonTimer.tick()) {
      performLeftMouseClick()
    }

    world.tick(camera)
    worldRenderer.tick(camera, world.renderDistance)

    if debugOverlay != null
    then
      debugOverlay.updateContent(
        DebugOverlay.Content.fromCamera(
          camera,
          viewDistance,
          worldRenderer.regularChunkBufferFragmentation,
          worldRenderer.transmissiveChunkBufferFragmentation
        )
      )

    for s <- overlays do s.tick()

  private def updatedMousePicker(): Option[(BlockState, BlockRelWorld, Option[Int])] =
    if isPaused || isInPopup
    then None
    else
      val screenCoords =
        if moveWithMouse
        then new Vector2f(0, 0)
        else mouse.normalizedScreenCoords(window.windowSize)

      // TODO: make it possible to place water on top of a water block (maybe by performing an extra ray trace)
      for
        ray <- Ray.fromScreen(camera, screenCoords)
        hit <- mousePicker.trace(ray, c => world.getBlock(c).blockType.isSolid)
      yield (world.getBlock(hit._1), hit._1, hit._2)

  private def performLeftMouseClick(): Unit =
    selectedBlockAndSide match
      case Some((state, coords, _)) =>
        if state.blockType != Block.Air
        then world.removeBlock(coords)
      case _ =>

  private def performRightMouseClick(): Unit =
    selectedBlockAndSide match
      case Some((state, coords, Some(side))) =>
        val coordsInFront = coords.offset(NeighborOffsets(side))

        state.blockType match
          case Block.Tnt => explode(coords)
          case _         => tryPlacingBlockAt(coordsInFront)
      case _ =>

  private def tryPlacingBlockAt(coords: BlockRelWorld): Unit =
    if !world.getBlock(coords).blockType.isSolid
    then
      val blockType = player.blockInHand
      val state = new BlockState(blockType)

      val collides = world.collisionDetector.collides(
        blockType.bounds(state.metadata),
        BlockCoords(coords).toCylCoords,
        player.bounds,
        CylCoords(camera.position)
      )

      if !collides
      then world.setBlock(coords, state)

  private def explode(coords: BlockRelWorld): Unit =
    for dy <- -1 to 1 do
      for offset <- NeighborOffsets.all do
        val c = coords.offset(offset).offset(0, dy, 0)
        world.setBlock(c, BlockState.Air)

    world.setBlock(coords, BlockState.Air)

  override def unload(): Unit =
    setMouseCursorInvisible(false)

    saveWorldInfo()

    for s <- overlays do s.unload()

    world.unload()
    worldRenderer.unload()
    crosshairVAO.free()
    crosshairShader.free()
    toolbar.unload()
    blockInHandRenderer.unload()

    if debugOverlay != null
    then debugOverlay.unload()

  private def viewDistance: Double = world.renderDistance

  private def makeBlockInHandRenderer(world: World, camera: Camera): GuiBlockRenderer =
    val renderer = GuiBlockRenderer(1, 1)
    renderer.setViewMatrix(makeBlockInHandViewMatrix)
    renderer.setWindowAspectRatio(window.aspectRatio)
    renderer

  private def makeCrosshairVAO: VAO = VAO
    .builder()
    .addVertexVbo(4)(
      _.floats(0, 2),
      _.fillFloats(0, Seq(0, 0.03f, 0, -0.03f, -0.03f, 0, 0.03f, 0))
    )
    .finish(4)

  private def makeToolbar(player: Player): Toolbar =
    val location = LocationInfo(-4.5f * 0.2f, -0.83f - 0.095f, 2 * 0.9f, 2 * 0.095f)

    val toolbar = new Toolbar(location, player.inventory)
    toolbar.setSelectedIndex(player.selectedItemSlot)
    toolbar.setWindowAspectRatio(window.aspectRatio)
    toolbar

  private def makeCameraProjection =
    val far = world.size.worldSize match
      case 0 => 100000f
      case 1 => 10000f
      case _ => 1000f

    new CameraProjection(70f, window.aspectRatio, 0.02f, far)

  private def makeBlockInHandViewMatrix =
    new Matrix4f()
      .translate(0, 0, -2f)
      .rotateZ(-3.1415f / 12)
      .rotateX(3.1415f / 6)
      .translate(0, -0.25f, 0)

  private def makePlayer(playerNbt: CompoundTag): Player =
    if playerNbt != null
    then Player.fromNBT(playerNbt)
    else
      val startX = (math.random() * 100 - 50).toInt
      val startZ = (math.random() * 100 - 50).toInt
      val startY = world.getHeight(startX, startZ) + 4
      Player.atStartPos(BlockCoords(startX, startY, startZ).toCylCoords)
