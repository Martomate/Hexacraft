package hexagon.scene

import java.io.File

import hexagon.block.BlockState
import hexagon.event.{KeyEvent, MouseClickEvent, ScrollEvent}
import hexagon.gui.comp.{GUITransformation, LocationInfo}
import hexagon.gui.inventory.{GUIBlocksRenderer, Toolbar}
import hexagon.gui.menu.pause.PauseMenu
import hexagon.renderer.{NoDepthTest, Renderer, VAOBuilder, VBO}
import hexagon.resource.Shader
import hexagon.world.WorldSettings
import hexagon.world.coord.{BlockCoords, CylCoords, RayTracer}
import hexagon.world.render.WorldRenderer
import hexagon.world.storage.World
import hexagon.{Camera, HexBox, Main}
import org.joml.{Matrix4f, Vector2f}
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW._
import org.lwjgl.opengl.GL11

import scala.collection.Seq


class GameScene(saveFolder: File, worldSettings: WorldSettings) extends Scene {
  // Camera, player, mouse-picker, world, etc.

  private val blockShader: Shader = Shader.get("block").get
  private val blockSideShader: Shader = Shader.get("blockSide").get
  private val guiBlockShader: Shader = Shader.get("gui_block").get
  private val guiBlockSideShader: Shader = Shader.get("gui_blockSide").get
  private val selectedBlockShader: Shader = Shader.get("selectedBlock").get
  private val skyShader: Shader = Shader.get("sky").get
  private val crosshairShader: Shader = Shader.get("crosshair").get

  private val crosshairVAO = new VAOBuilder(4).addVBO(VBO(4).floats(0, 2).create().fillFloats(0, Seq(0, 0.02f, 0, -0.02f, -0.02f, 0, 0.02f, 0))).create()
  private val crosshairRenderer = new Renderer(crosshairVAO, GL11.GL_LINES) with NoDepthTest


  val world = new World(saveFolder, worldSettings)
  val worldRenderer = new WorldRenderer(world)

  val camera = new Camera(70f, Main.windowSize.x.toFloat / Main.windowSize.y, 0.02f, 1000f, world)
  val mousePicker = new RayTracer(world, camera, 7)
  val playerInputHandler = new PlayerInputHandler(world.player)

  private val toolbar = new Toolbar(LocationInfo(0.05f, 0.035f, 0.9f, 0.095f), world.player.inventory)
  private val blockInHandRenderer = new GUIBlocksRenderer(1, xOff = 1.2f, yOff = -0.8f)(_ => world.player.blockInHand)
  blockInHandRenderer.setViewMatrix(new Matrix4f().translate(0, 0, -2f).rotateZ(-3.1415f / 12).rotateX(3.1415f / 6).translate(0, -0.25f, 0))

  private var leftMouseButtonDown = false
  private var rightMouseButtonDown = false
  private var leftMouseButtonCountdown = 0
  private var rightMouseButtonCountdown = 0

  private var isPaused: Boolean = false

  private var debugScene: DebugScene = _
  
  setUniforms()
  toolbar.setSelectedIndex(world.player.selectedItemSlot)
  setUseMouse(true)
  
  override def onReloadedResources(): Unit = {
    setUniforms()
    world.columns.values.foreach(_.chunks.values.foreach(_.requestRenderUpdate()))
  }

  private def setUniforms(): Unit = {
    camera.setProjMatrix(blockShader)
    camera.setProjMatrix(blockSideShader)
    camera.setProjMatrix(guiBlockShader)
    camera.setProjMatrix(guiBlockSideShader)
    camera.setProjMatrix(selectedBlockShader)

    blockShader.setUniform1f("totalSize", world.totalSize)
    blockSideShader.setUniform1f("totalSize", world.totalSize)
    selectedBlockShader.setUniform1f("totalSize", world.totalSize)

    skyShader.setUniformMat4("invProjMatr", camera.invProjMatr)

    Shader.foreach(_.setUniform2f("windowSize", Main.windowSize.x, Main.windowSize.y))
  }

  override def onKeyEvent(event: KeyEvent): Boolean = {
    if (event.action == GLFW_PRESS) {
      event.key match {
        case GLFW_KEY_ESCAPE =>
          Main.pushScene(new PauseMenu(this))
          setPaused(true)
        case GLFW_KEY_M =>
          setUseMouse(!playerInputHandler.moveWithMouse)
        case GLFW_KEY_F =>
          playerInputHandler.player.flying = !playerInputHandler.player.flying
        case GLFW_KEY_F7 =>
          if (debugScene != null) {
            debugScene.unload()
            debugScene = null
          } else debugScene = new DebugScene(this)
        case key if key >= GLFW_KEY_1 && key <= GLFW_KEY_9 =>
          val idx = key - GLFW_KEY_1
          playerInputHandler.player.selectedItemSlot = idx
          blockInHandRenderer.updateContent()
          toolbar.setSelectedIndex(idx)
        case _ =>
      }
    }
    true
  }

  def setUseMouse(useMouse: Boolean): Unit = {
    playerInputHandler.moveWithMouse = useMouse
    setMouseCursorInvisible(playerInputHandler.moveWithMouse)
    Main.updateMousePos()
  }

  override def onScrollEvent(event: ScrollEvent): Boolean = {
    if (playerInputHandler.moveWithMouse) {
      val dy = -math.signum(event.yoffset).toInt
      if (dy != 0) {
        val itemSlot = (playerInputHandler.player.selectedItemSlot + dy + 9) % 9
        playerInputHandler.player.selectedItemSlot = itemSlot
        blockInHandRenderer.updateContent()
        toolbar.setSelectedIndex(itemSlot)
      }
      true
    } else super.onScrollEvent(event)
  }

  def setPaused(paused: Boolean): Unit = {
    if (paused != isPaused) {
      isPaused = paused

      setMouseCursorInvisible(!paused && playerInputHandler.moveWithMouse)
    }
  }

  private def setMouseCursorInvisible(invisible: Boolean): Unit = {
    glfwSetInputMode(Main.window, GLFW_CURSOR, if (invisible) GLFW_CURSOR_DISABLED else GLFW_CURSOR_NORMAL)
  }

  override def onMouseClickEvent(event: MouseClickEvent): Boolean = {
    event.button match {
      case 0 =>
        rightMouseButtonDown = event.action != GLFW.GLFW_RELEASE
      case 1 =>
        leftMouseButtonDown = event.action != GLFW.GLFW_RELEASE
      case _ =>
    }
    true
  }

  override def windowResized(width: Int, height: Int): Unit = {
    camera.aspect = width.toFloat / height
    camera.updateProjMatrix()

    camera.setProjMatrix(blockShader)
    camera.setProjMatrix(blockSideShader)
    camera.setProjMatrix(guiBlockShader)
    camera.setProjMatrix(guiBlockSideShader)
    camera.setProjMatrix(selectedBlockShader)

    skyShader.setUniformMat4("invProjMatr", camera.invProjMatr)
  }
  
  override def windowTitle: String = ""

  override def render(transformation: GUITransformation): Unit = {
    // render world etc.
    worldRenderer.render(camera)

    if (!isPaused && playerInputHandler.moveWithMouse) {
      crosshairShader.enable()
      crosshairRenderer.render()
    }

    blockInHandRenderer.render(transformation)
    toolbar.render(transformation)

    if (debugScene != null) debugScene.render(transformation)
  }

  override def tick(): Unit = {
    if (!isPaused) playerInputHandler.tick()
    camera.setPositionAndRotation(playerInputHandler.player)
    camera.updateCoords()
    camera.updateViewMatrix()
    camera.updateUniforms(blockShader)
    camera.updateUniforms(blockSideShader)
    camera.updateUniforms(selectedBlockShader)
    skyShader.setUniformMat4("invViewMatr", camera.invViewMatr)
    skyShader.setUniform3f("sun", 0, 1, -1)
    blockShader.setUniform3f("sun", 0, 1, -1)
    blockSideShader.setUniform3f("sun", 0, 1, -1)

    // MousePicker
    mousePicker.setRayFromScreen(if (!playerInputHandler.moveWithMouse) Main.normalizedMousePos else new Vector2f(0, 0))
    worldRenderer.setSelectedBlockAndSide(if (!isPaused) mousePicker.trace(c => world.getBlock(c).isDefined) else None)

    if (leftMouseButtonCountdown == 0) {
      if (leftMouseButtonDown) {
        leftMouseButtonCountdown = 10
        worldRenderer.getSelectedBlockAndSide match {
          case Some((coords1, Some(side))) =>
            val offset = BlockState.neighborOffsets(side)
            val coords = coords1.offset(offset._1, offset._2, offset._3)
            if (world.getBlock(coords).isEmpty) {
              val blockType = playerInputHandler.player.blockInHand
              val skewCoords = BlockCoords(coords.x, coords.y, coords.z, world).toSkewCylCoord
              val state = new BlockState(coords, blockType)
              if (!HexBox.collides(blockType.bounds(state), skewCoords, playerInputHandler.player.bounds, CylCoords(camera.position, world))) {
                world.setBlock(state)
              }
            }
          case _ =>
        }
      }
    } else {
      leftMouseButtonCountdown -= 1
    }

    if (rightMouseButtonCountdown == 0) {
      if (rightMouseButtonDown) {
        rightMouseButtonCountdown = 10
        worldRenderer.getSelectedBlockAndSide match {
          case Some((coords, _)) =>
            if (world.getBlock(coords).isDefined) {
              world.removeBlock(coords)
            }
          case _ =>
        }
      }
    } else {
      rightMouseButtonCountdown -= 1
    }
    world.tick(camera)

    if (debugScene != null) debugScene.tick()
  }

  override def unload(): Unit = {
    world.unload()
    worldRenderer.unload()
    crosshairVAO.free()
    setMouseCursorInvisible(false)
    if (debugScene != null) debugScene.unload()
  }
}
