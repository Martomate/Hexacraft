package com.martomate.hexacraft.game

import java.io.File

import com.martomate.hexacraft.event.{KeyEvent, MouseClickEvent, ScrollEvent}
import com.martomate.hexacraft.game.debug.{DebugInfoProvider, DebugScene}
import com.martomate.hexacraft.game.pause.{PausableScene, PauseMenu}
import com.martomate.hexacraft.gui.comp.GUITransformation
import com.martomate.hexacraft.gui.inventory.{GUIBlocksRenderer, Toolbar}
import com.martomate.hexacraft.gui.location.LocationInfoIdentity
import com.martomate.hexacraft.renderer._
import com.martomate.hexacraft.resource.Shader
import com.martomate.hexacraft.scene.{GameWindowExtended, Scene}
import com.martomate.hexacraft.util.TickableTimer
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.camera.{Camera, CameraProjection}
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, NeighborOffsets}
import com.martomate.hexacraft.world.entity.loader.EntityModelLoader
import com.martomate.hexacraft.world.entity.player.PlayerEntity
import com.martomate.hexacraft.world.entity.player.ai.PlayerAIFactory
import com.martomate.hexacraft.world.entity.sheep.SheepEntity
import com.martomate.hexacraft.world.entity.sheep.ai.SheepAIFactory
import com.martomate.hexacraft.world.player.Player
import com.martomate.hexacraft.world.render.WorldRenderer
import com.martomate.hexacraft.world.settings.{WorldSettings, WorldSettingsProviderFromFile}
import com.martomate.hexacraft.world.{RayTracer, World}
import org.joml.{Matrix4f, Vector2f}
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW._
import org.lwjgl.opengl.GL11

class GameScene(saveFolder: File, worldSettings: WorldSettings)(implicit window: GameWindowExtended) extends Scene with PausableScene with DebugInfoProvider {
  // Camera, player, mouse-picker, world, etc.

  private val blockShader: Shader = Shader.get("block").get
  private val blockSideShader: Shader = Shader.get("blockSide").get
  private val entityShader: Shader = Shader.get("entity").get
  private val entitySideShader: Shader = Shader.get("entitySide").get
  private val guiBlockShader: Shader = Shader.get("gui_block").get
  private val guiBlockSideShader: Shader = Shader.get("gui_blockSide").get
  private val selectedBlockShader: Shader = Shader.get("selectedBlock").get
  private val skyShader: Shader = Shader.get("sky").get
  private val crosshairShader: Shader = Shader.get("crosshair").get

  private val shadersNeedingTotalSize: Seq[Shader] = Seq(blockShader, blockSideShader, entityShader, entitySideShader, selectedBlockShader)
  private val shadersNeedingProjMatrix: Seq[Shader] = Seq(blockShader, blockSideShader, entityShader, entitySideShader, guiBlockShader, guiBlockSideShader, selectedBlockShader)
  private val shadersNeedingCamera: Seq[Shader] = Seq(blockShader, blockSideShader, entityShader, entitySideShader, selectedBlockShader)
  private val shadersNeedingSun: Seq[Shader] = Seq(skyShader, blockShader, blockSideShader, entityShader, entitySideShader)

  private val crosshairVAO: VAO = makeCrosshairVAO
  private val crosshairRenderer: Renderer = new Renderer(crosshairVAO, GL11.GL_LINES) with NoDepthTest

  private val world = new World(new WorldSettingsProviderFromFile(saveFolder, worldSettings))
  import world.size.impl

  private val worldRenderer: WorldRenderer = new WorldRenderer(world)

  val camera: Camera = new Camera(new CameraProjection(70f, window.aspectRatio, 0.02f, 1000f))
  private val mousePicker: RayTracer = new RayTracer(world, camera, 7)
  private val playerInputHandler: PlayerInputHandler = new PlayerInputHandler(window.mouse, window.keyboard, world.player)

  private val toolbar: Toolbar = makeToolbar(world.player)
  private val blockInHandRenderer: GUIBlocksRenderer = makeBlockInHandRenderer(world, camera)

  private val rightMouseButtonTimer: TickableTimer = TickableTimer(10, initActive = false)
  private val leftMouseButtonTimer: TickableTimer = TickableTimer(10, initActive = false)

  private val entityModelLoader = new EntityModelLoader

  private var isPaused: Boolean = false

  private var debugScene: DebugScene = _

  setUniforms()
  setUseMouse(true)
  
  override def onReloadedResources(): Unit = {
    setUniforms()
    world.onReloadedResources()
  }

  private def setUniforms(): Unit = {
    setProjMatrixForAll()

    for (shader <- shadersNeedingTotalSize)
      shader.setUniform1i("totalSize", world.size.totalSize)

    skyShader.setUniformMat4("invProjMatr", camera.proj.invMatrix)

    Shader.foreach(_.setUniform2f("windowSize", window.windowSize.x.toFloat, window.windowSize.y.toFloat))
  }

  private def setProjMatrixForAll(): Unit = {
    for (shader <- shadersNeedingProjMatrix)
      camera.setProjMatrix(shader)
  }

  override def onKeyEvent(event: KeyEvent): Boolean = {
    if (event.action == GLFW_PRESS) {
      event.key match {
        case GLFW_KEY_B =>
          val newCoords = camera.blockCoords.offset(0, -4, 0)
          if (world.getBlock(newCoords).blockType == Blocks.Air) world.setBlock(newCoords, new BlockState(world.player.blockInHand))
        case GLFW_KEY_ESCAPE =>
          window.scenes.pushScene(new PauseMenu(this))
          setPaused(true)
        case GLFW_KEY_M =>
          setUseMouse(!playerInputHandler.moveWithMouse)
        case GLFW_KEY_F =>
          playerInputHandler.player.flying = !playerInputHandler.player.flying
        case GLFW_KEY_F7 =>
          setDebugScreenVisible(debugScene == null)
        case key if key >= GLFW_KEY_1 && key <= GLFW_KEY_9 =>
          val idx = key - GLFW_KEY_1
          setSelectedItemSlot(idx)
        case GLFW_KEY_P =>
          world.addEntity(PlayerEntity.atStartPos(CylCoords(world.player.position), world, PlayerAIFactory, entityModelLoader.load("player")))
        case GLFW_KEY_L =>
          world.addEntity(SheepEntity.atStartPos(CylCoords(world.player.position), world, SheepAIFactory, entityModelLoader.load("sheep")))
        case GLFW_KEY_K =>
          world.removeAllEntities()
        case _ =>
      }
    }
    true
  }

  private def setDebugScreenVisible(visible: Boolean): Unit = {
    if (visible) {
      if (debugScene == null) debugScene = new DebugScene(this)
    } else {
      if (debugScene != null) debugScene.unload()
      debugScene = null
    }
  }

  def setUseMouse(useMouse: Boolean): Unit = {
    playerInputHandler.moveWithMouse = useMouse
    setMouseCursorInvisible(playerInputHandler.moveWithMouse)
    window.resetMousePos()
  }

  override def onScrollEvent(event: ScrollEvent): Boolean = {
    if (playerInputHandler.moveWithMouse) {
      val dy = -math.signum(event.yoffset).toInt
      if (dy != 0) {
        val itemSlot = (playerInputHandler.player.selectedItemSlot + dy + 9) % 9
        setSelectedItemSlot(itemSlot)
      }
      true
    } else super.onScrollEvent(event)
  }

  private def setSelectedItemSlot(itemSlot: Int): Unit = {
    playerInputHandler.player.selectedItemSlot = itemSlot
    blockInHandRenderer.updateContent()
    toolbar.setSelectedIndex(itemSlot)
  }

  def setPaused(paused: Boolean): Unit = {
    if (paused != isPaused) {
      isPaused = paused

      setMouseCursorInvisible(!paused && playerInputHandler.moveWithMouse)
    }
  }

  private def setMouseCursorInvisible(invisible: Boolean): Unit = {
    window.setCursorLayout(if (invisible) GLFW_CURSOR_DISABLED else GLFW_CURSOR_NORMAL)
  }

  override def onMouseClickEvent(event: MouseClickEvent): Boolean = {
    event.button match {
      case 0 =>
        leftMouseButtonTimer.active = event.action != GLFW.GLFW_RELEASE
      case 1 =>
        rightMouseButtonTimer.active = event.action != GLFW.GLFW_RELEASE
      case _ =>
    }
    true
  }

  override def windowResized(width: Int, height: Int): Unit = {
    camera.proj.aspect = width.toFloat / height
    camera.updateProjMatrix()

    setProjMatrixForAll()

    skyShader.setUniformMat4("invProjMatr", camera.proj.invMatrix)

    if (debugScene != null) debugScene.windowResized(width, height)
  }
  
  override def windowTitle: String = ""

  override def render(transformation: GUITransformation): Unit = {
    worldRenderer.render(camera)

    renderCrosshair()

    blockInHandRenderer.render(transformation)
    toolbar.render(transformation)

    if (debugScene != null) debugScene.render(transformation)
  }

  private def renderCrosshair(): Unit = {
    if (!isPaused && playerInputHandler.moveWithMouse) {
      crosshairShader.enable()
      crosshairRenderer.render()
    }
  }

  override def tick(): Unit = {
    if (!isPaused) playerInputHandler.tick()

    camera.setPositionAndRotation(playerInputHandler.player)
    camera.updateCoords()
    camera.updateViewMatrix()
    for (shader <- shadersNeedingCamera)
      camera.updateUniforms(shader)

    skyShader.setUniformMat4("invViewMatr", camera.view.invMatrix)
    for (shader <- shadersNeedingSun)
      shader.setUniform3f("sun", 0, 1, -1)

    blockInHandRenderer.updateContent()

    updateMousePicker()

    if (rightMouseButtonTimer.tick()) {
      performRightMouseClick()
    }
    if (leftMouseButtonTimer.tick()) {
      performLeftMouseClick()
    }

    world.tick(camera)
    worldRenderer.tick(camera)
    if (debugScene != null) debugScene.tick()
  }

  private def updateMousePicker(): Unit = {
    mousePicker.setRayFromScreen(if (!playerInputHandler.moveWithMouse) window.normalizedMousePos else new Vector2f(0, 0))
    worldRenderer.selectedBlockAndSide = if (!isPaused) mousePicker.trace(c => world.getBlock(c).blockType != Blocks.Air) else None
  }

  private def performLeftMouseClick(): Unit = {
    worldRenderer.selectedBlockAndSide match {
      case Some((coords, _)) =>
        if (world.getBlock(coords).blockType != Blocks.Air) {
          world.removeBlock(coords)
        }
      case _ =>
    }
  }

  private def performRightMouseClick(): Unit = {
    worldRenderer.selectedBlockAndSide match {
      case Some((coords1, Some(side))) =>
        val offset = NeighborOffsets(side)
        val coords = coords1.offset(offset)
        tryPlacingBlockAt(coords)
      case _ =>
    }
  }

  private def tryPlacingBlockAt(coords: BlockRelWorld): Unit = {
    if (world.getBlock(coords).blockType == Blocks.Air) {
      val blockType = playerInputHandler.player.blockInHand
      val skewCoords = BlockCoords(coords).toSkewCylCoords
      val state = new BlockState(blockType)
      if (!world.collisionDetector.collides(blockType.bounds(state.metadata), skewCoords, playerInputHandler.player.bounds, CylCoords(camera.position))) {
        world.setBlock(coords, state)
      }
    }
  }

  override def unload(): Unit = {
    world.unload()
    worldRenderer.unload()
    crosshairVAO.free()
    toolbar.unload()
    blockInHandRenderer.unload()
    setMouseCursorInvisible(false)
    if (debugScene != null) debugScene.unload()
  }

  override def viewDistance: Double = world.renderDistance

  private def makeBlockInHandRenderer(world: World, camera: Camera): GUIBlocksRenderer = {
    val renderer = new GUIBlocksRenderer(1, xOff = 1.5f, yOff = -0.9f, initBrightnessFunc = (_, _) => world.getBrightness(camera.blockCoords))(_ => world.player.blockInHand)
    renderer.setViewMatrix(new Matrix4f().translate(0, 0, -2f).rotateZ(-3.1415f / 12).rotateX(3.1415f / 6).translate(0, -0.25f, 0))
    renderer
  }

  private def makeCrosshairVAO: VAO = new VAOBuilder(4).addVBO(
    VBOBuilder(4).floats(0, 2).create().fillFloats(0,
      Seq(0, 0.02f, 0, -0.02f, -0.02f, 0, 0.02f, 0)
    )
  ).create()

  private def makeToolbar(player: Player): Toolbar = {
    val toolbar = new Toolbar(new LocationInfoIdentity(-4.5f * 0.2f, -0.83f - 0.095f, 2 * 0.9f, 2 * 0.095f), player.inventory)
    toolbar.setSelectedIndex(player.selectedItemSlot)
    toolbar
  }
}
