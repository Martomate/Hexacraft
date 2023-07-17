package com.martomate.hexacraft.game

import com.martomate.hexacraft.{GameKeyboard, GameMouse, GameWindow}
import com.martomate.hexacraft.game.inventory.{GUIBlocksRenderer, InventoryBox, Toolbar}
import com.martomate.hexacraft.gui.*
import com.martomate.hexacraft.gui.comp.{Component, GUITransformation}
import com.martomate.hexacraft.infra.gpu.OpenGL
import com.martomate.hexacraft.infra.window.{CursorMode, KeyAction, KeyboardKey, MouseAction, MouseButton}
import com.martomate.hexacraft.renderer.{NoDepthTest, Renderer, Shader, TextureArray, VAO}
import com.martomate.hexacraft.util.{ResourceWrapper, TickableTimer, Tracker}
import com.martomate.hexacraft.world.{World, WorldProvider}
import com.martomate.hexacraft.world.block.{Block, BlockLoader, Blocks, BlockState}
import com.martomate.hexacraft.world.camera.{Camera, CameraProjection}
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, NeighborOffsets}
import com.martomate.hexacraft.world.entity.{EntityBaseData, EntityModel, EntityModelLoader}
import com.martomate.hexacraft.world.entity.player.ControlledPlayerEntity
import com.martomate.hexacraft.world.player.Player
import com.martomate.hexacraft.world.ray.{Ray, RayTracer}
import com.martomate.hexacraft.world.render.WorldRenderer
import com.martomate.hexacraft.world.settings.WorldInfo

import com.flowpowered.nbt.CompoundTag
import org.joml.{Matrix4f, Vector2f}
import org.joml.Vector2d
import org.joml.Vector3f
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
    blockLoader: BlockLoader,
    Blocks: Blocks
)(using WindowExtras)
    extends Scene:

  TextureArray.registerTextureArray("blocks", 32, new ResourceWrapper(blockLoader.reloadAllBlockTextures()))

  private val crosshairShader = new CrosshairShader()
  private val crosshairVAO: VAO = makeCrosshairVAO
  private val crosshairRenderer: Renderer =
    new Renderer(crosshairVAO, OpenGL.PrimitiveMode.Lines) with NoDepthTest

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
    new PlayerPhysicsHandler(player, world.collisionDetector)

  private var selectedBlockAndSide: Option[(BlockRelWorld, Option[Int])] = None

  private val toolbar: Toolbar = makeToolbar(player)
  private val blockInHandRenderer: GUIBlocksRenderer = makeBlockInHandRenderer(world, camera)

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
    worldProvider.saveState(worldTag, "world.dat")

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

      if world.getBlock(newCoords).blockType == Blocks.Air
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
      setSelectedItemSlot(digit)
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
      then debugOverlay = new DebugOverlay(window.aspectRatio)
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
        case ScrollEvent(_, yOffset) if !isPaused && !isInPopup && moveWithMouse =>
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
    blockInHandRenderer.updateContent()
    toolbar.setSelectedIndex(itemSlot)

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

    if debugOverlay != null
    then debugOverlay.windowResized(width, height)

    crosshairShader.setWindowAspectRatio(aspectRatio)

  override def framebufferResized(width: Int, height: Int): Unit =
    worldRenderer.framebufferResized(width, height)

  override def render(transformation: GUITransformation)(using GameWindow): Unit =
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
      crosshairRenderer.render()

  override def tick(): Unit =
    val maxSpeed = playerInputHandler.maxSpeed
    if !isPaused && !isInPopup then
      playerInputHandler.tick(if moveWithMouse then mouse.moved else new Vector2d(), maxSpeed)
    if !isPaused then playerPhysicsHandler.tick(maxSpeed)

    camera.setPositionAndRotation(player.position, player.rotation)
    camera.updateCoords()
    camera.updateViewMatrix()

    blockInHandRenderer.updateContent()

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
    then debugOverlay.updateContent(DebugOverlay.Content.fromCamera(camera, viewDistance))

    for s <- overlays do s.tick()

  private def updatedMousePicker(): Option[(BlockRelWorld, Option[Int])] =
    if isPaused || isInPopup
    then None
    else
      val screenCoords =
        if moveWithMouse
        then new Vector2f(0, 0)
        else mouse.normalizedScreenCoords(window.windowSize)
      for
        ray <- Ray.fromScreen(camera, screenCoords)
        hit <- mousePicker.trace(ray, c => world.getBlock(c).blockType != Blocks.Air)
      yield hit

  private def performLeftMouseClick(): Unit =
    selectedBlockAndSide match
      case Some((coords, _)) =>
        if world.getBlock(coords).blockType != Blocks.Air
        then world.removeBlock(coords)
      case _ =>

  private def performRightMouseClick(): Unit =
    selectedBlockAndSide match
      case Some((coords, Some(side))) =>
        val coordsInFront = coords.offset(NeighborOffsets(side))
        tryPlacingBlockAt(coordsInFront)
      case _ =>

  private def tryPlacingBlockAt(coords: BlockRelWorld): Unit =
    if world.getBlock(coords).blockType == Blocks.Air
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

  private def makeBlockInHandRenderer(world: World, camera: Camera): GUIBlocksRenderer =
    val blockProvider = () => player.blockInHand
    val offsetFunc = () => (1.5f, -0.9f)
    val brightnessFunc = () => world.getBrightness(camera.blockCoords)

    val viewMatrix = makeBlockInHandViewMatrix

    val renderer = GUIBlocksRenderer.withSingleSlot(blockProvider, offsetFunc, brightnessFunc)
    renderer.setViewMatrix(viewMatrix)
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
