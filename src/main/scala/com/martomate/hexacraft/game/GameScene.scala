package com.martomate.hexacraft.game

import com.martomate.hexacraft.game.inventory.{GUIBlocksRenderer, InventoryScene, Toolbar}
import com.martomate.hexacraft.gui.*
import com.martomate.hexacraft.gui.comp.GUITransformation
import com.martomate.hexacraft.renderer.*
import com.martomate.hexacraft.util.TickableTimer
import com.martomate.hexacraft.world.{DebugInfoProvider, World, WorldProvider}
import com.martomate.hexacraft.world.block.{Block, Blocks, BlockState}
import com.martomate.hexacraft.world.camera.{Camera, CameraProjection}
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, NeighborOffsets}
import com.martomate.hexacraft.world.entity.EntityModelLoader
import com.martomate.hexacraft.world.player.Player
import com.martomate.hexacraft.world.ray.{Ray, RayTracer}
import com.martomate.hexacraft.world.render.WorldRenderer

import org.joml.{Matrix4f, Vector2f}
import org.joml.Vector2d
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11

class GameScene(worldProvider: WorldProvider)(using window: GameWindowExtended, Blocks: Blocks)
    extends Scene
    with DebugInfoProvider:

  private val crosshairShader: Shader = Shader.get(Shaders.ShaderNames.Crosshair).get
  private val crosshairVAO: VAO = makeCrosshairVAO
  private val crosshairRenderer: Renderer =
    new Renderer(crosshairVAO, GL11.GL_LINES) with NoDepthTest

  private val world = World(worldProvider)
  import world.size.impl

  private val worldRenderer: WorldRenderer = new WorldRenderer(world, window.framebufferSize)
  world.addChunkAddedOrRemovedListener(worldRenderer)

  val camera: Camera = new Camera(makeCameraProjection)

  private val mousePicker: RayTracer = new RayTracer(world, camera, 7)
  private val playerInputHandler: PlayerInputHandler =
    new PlayerInputHandler(window.keyboard, world.player)
  private val playerPhysicsHandler: PlayerPhysicsHandler =
    new PlayerPhysicsHandler(world.player, world.collisionDetector)

  private var selectedBlockAndSide: Option[(BlockRelWorld, Option[Int])] = None

  private val toolbar: Toolbar = makeToolbar(world.player)
  private val blockInHandRenderer: GUIBlocksRenderer = makeBlockInHandRenderer(world, camera)

  private val rightMouseButtonTimer: TickableTimer = TickableTimer(10, initActive = false)
  private val leftMouseButtonTimer: TickableTimer = TickableTimer(10, initActive = false)

  private var moveWithMouse: Boolean = false
  private var isPaused: Boolean = false
  private var isInPopup: Boolean = false

  private var debugScene: DebugScene = _

  setUniforms()
  setUseMouse(true)

  override def onReloadedResources(): Unit =
    setUniforms()
    world.onReloadedResources()

  private def setUniforms(): Unit =
    setProjMatrixForAll()

    worldRenderer.onTotalSizeChanged(world.size.totalSize)

    Shader.foreach(
      _.setUniform2f("windowSize", window.windowSize.x.toFloat, window.windowSize.y.toFloat)
    )

  private def setProjMatrixForAll(): Unit =
    worldRenderer.onProjMatrixChanged(camera)

  override def onKeyEvent(event: Event.KeyEvent): Boolean =
    if event.action == Event.KeyAction.Press
    then
      event.key match
        case GLFW_KEY_B =>
          val newCoords = camera.blockCoords.offset(0, -4, 0)

          if world.getBlock(newCoords).blockType == Blocks.Air
          then world.setBlock(newCoords, new BlockState(world.player.blockInHand))
        case GLFW_KEY_ESCAPE =>
          window.scenes.pushScene(new PauseMenu(this.setPaused))
          setPaused(true)
        case GLFW_KEY_E =>
          if !isPaused
          then
            val closeScene: () => Unit = () => {
              window.scenes.popScenesUntil(_ == this)
              isInPopup = false
              setUseMouse(true)
            }

            setUseMouse(false)
            isInPopup = true
            window.scenes.pushScene(new InventoryScene(world.player.inventory, closeScene))
        case GLFW_KEY_M =>
          setUseMouse(!moveWithMouse)
        case GLFW_KEY_F =>
          world.player.flying = !world.player.flying
        case GLFW_KEY_F7 =>
          setDebugScreenVisible(debugScene == null)
        case key if key >= GLFW_KEY_1 && key <= GLFW_KEY_9 =>
          val idx = key - GLFW_KEY_1
          setSelectedItemSlot(idx)
        case GLFW_KEY_P =>
          val startPos = CylCoords(world.player.position)

          world.addEntity(world.entityRegistry.get("player").get.atStartPos(startPos))
        case GLFW_KEY_L =>
          val startPos = CylCoords(world.player.position)

          world.addEntity(world.entityRegistry.get("sheep").get.atStartPos(startPos))
        case GLFW_KEY_K =>
          world.removeAllEntities()
        case _ =>
    true

  private def setDebugScreenVisible(visible: Boolean): Unit =
    if visible
    then
      if debugScene == null
      then debugScene = new DebugScene(this)
    else
      if debugScene != null
      then debugScene.unload()
      debugScene = null

  private def setUseMouse(useMouse: Boolean): Unit =
    moveWithMouse = useMouse
    setMouseCursorInvisible(moveWithMouse)
    window.resetMousePos()

  override def onScrollEvent(event: Event.ScrollEvent): Boolean =
    if !isPaused && !isInPopup && moveWithMouse
    then
      val dy = -math.signum(event.yOffset).toInt
      if dy != 0
      then
        val itemSlot = (world.player.selectedItemSlot + dy + 9) % 9
        setSelectedItemSlot(itemSlot)
      true
    else super.onScrollEvent(event)

  private def setSelectedItemSlot(itemSlot: Int): Unit =
    world.player.selectedItemSlot = itemSlot
    blockInHandRenderer.updateContent()
    toolbar.setSelectedIndex(itemSlot)

  def setPaused(paused: Boolean): Unit =
    if isPaused != paused
    then
      isPaused = paused
      setMouseCursorInvisible(!paused && moveWithMouse)

  private def setMouseCursorInvisible(invisible: Boolean): Unit =
    window.setCursorLayout(if invisible then GLFW_CURSOR_DISABLED else GLFW_CURSOR_NORMAL)

  override def onMouseClickEvent(event: Event.MouseClickEvent): Boolean =
    event.button match
      case Event.MouseButton.Left  => leftMouseButtonTimer.active = event.action != Event.MouseAction.Release
      case Event.MouseButton.Right => rightMouseButtonTimer.active = event.action != Event.MouseAction.Release
      case _                       =>
    true

  override def windowResized(width: Int, height: Int): Unit =
    camera.proj.aspect = width.toFloat / height
    camera.updateProjMatrix()

    setProjMatrixForAll()
    blockInHandRenderer.onWindowAspectRatioChanged(width.toFloat / height)
    toolbar.onWindowAspectRatioChanged(width.toFloat / height)

    if debugScene != null
    then debugScene.windowResized(width, height)

  override def framebufferResized(width: Int, height: Int): Unit =
    worldRenderer.framebufferResized(width, height)

  override def windowTitle: String = ""

  override def render(transformation: GUITransformation): Unit =
    worldRenderer.render(camera, new Vector3f(0, 1, -1), selectedBlockAndSide)

    renderCrosshair()

    blockInHandRenderer.render(transformation)
    toolbar.render(transformation)

    if debugScene != null
    then debugScene.render(transformation)

  private def renderCrosshair(): Unit =
    if !isPaused && !isInPopup && moveWithMouse
    then
      crosshairShader.enable()
      crosshairRenderer.render()

  override def tick(): Unit =
    val maxSpeed = playerInputHandler.maxSpeed
    if !isPaused && !isInPopup then
      playerInputHandler.tick(if moveWithMouse then window.mouse.moved else new Vector2d(), maxSpeed)
    if !isPaused then playerPhysicsHandler.tick(maxSpeed)

    camera.setPositionAndRotation(world.player.position, world.player.rotation)
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

    if debugScene != null
    then debugScene.tick()

  private def updatedMousePicker(): Option[(BlockRelWorld, Option[Int])] =
    if isPaused || isInPopup
    then None
    else
      val screenCoords =
        if moveWithMouse
        then new Vector2f(0, 0)
        else window.normalizedMousePos
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
      val blockType = world.player.blockInHand
      val state = new BlockState(blockType)

      val collides = world.collisionDetector.collides(
        blockType.bounds(state.metadata),
        BlockCoords(coords).toCylCoords,
        world.player.bounds,
        CylCoords(camera.position)
      )

      if !collides
      then world.setBlock(coords, state)

  override def unload(): Unit =
    setMouseCursorInvisible(false)

    world.unload()
    worldRenderer.unload()
    crosshairVAO.free()
    toolbar.unload()
    blockInHandRenderer.unload()

    if debugScene != null
    then debugScene.unload()

  override def viewDistance: Double = world.renderDistance

  private def makeBlockInHandRenderer(world: World, camera: Camera): GUIBlocksRenderer =
    val blockProvider = () => world.player.blockInHand
    val offsetFunc = () => (1.5f, -0.9f)
    val brightnessFunc = () => world.getBrightness(camera.blockCoords)

    val viewMatrix = makeBlockInHandViewMatrix

    val renderer = GUIBlocksRenderer.withSingleSlot(blockProvider, offsetFunc, brightnessFunc)
    renderer.setViewMatrix(viewMatrix)
    renderer.onWindowAspectRatioChanged(window.aspectRatio)
    renderer

  private def makeCrosshairVAO: VAO = new VAOBuilder(4)
    .addVBO(
      VBOBuilder(4)
        .floats(0, 2)
        .create()
        .fillFloats(0, Seq(0, 0.02f, 0, -0.02f, -0.02f, 0, 0.02f, 0))
    )
    .create()

  private def makeToolbar(player: Player): Toolbar =
    val location = LocationInfo(-4.5f * 0.2f, -0.83f - 0.095f, 2 * 0.9f, 2 * 0.095f)

    val toolbar = new Toolbar(location, player.inventory)
    toolbar.setSelectedIndex(player.selectedItemSlot)
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
