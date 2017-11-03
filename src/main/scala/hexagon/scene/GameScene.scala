package hexagon.scene

import java.io.File

import hexagon.{Camera, HexBox, Main}
import hexagon.block.BlockState
import hexagon.gui.menu.pause.PauseMenu
import hexagon.renderer.{NoDepthTest, Renderer, VAOBuilder, VBO}
import hexagon.resource.Shader
import hexagon.world.WorldSettings
import hexagon.world.coord.{BlockCoord, CylCoord, RayTracer}
import hexagon.world.render.WorldRenderer
import hexagon.world.storage.World
import org.joml.Vector2f
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW._
import org.lwjgl.opengl.GL11

import scala.collection.Seq


class GameScene(saveFolder: File, worldSettings: WorldSettings) extends Scene {
  // Camera, player, mousepicker, world, etc.

  val blockShader: Shader = Shader.get("block").get
  val blockSideShader: Shader = Shader.get("blockSide").get
  val selectedBlockShader: Shader = Shader.get("selectedBlock").get
  val skyShader: Shader = Shader.get("sky").get
  val crosshairShader: Shader = Shader.get("crosshair").get

  private val crosshairVAO = new VAOBuilder(8).addVBO(VBO(4).floats(0, 2).create().fillFloats(0, Seq(0, 0.02f, 0, -0.02f, -0.02f, 0, 0.02f, 0))).create()
  private val crosshairRenderer = new Renderer(crosshairVAO, GL11.GL_LINES) with NoDepthTest

  val world = new World(saveFolder, worldSettings)
  val worldRenderer = new WorldRenderer(world)

  val camera = new Camera(70f, Main.windowSize.x.toFloat / Main.windowSize.y, 0.02f, 1000f, world)
  val mousePicker = new RayTracer(world, camera, 7)
  val playerInputHandler = new PlayerInputHandler(world.player)

  private var leftMouseButtonDown = false
  private var rightMouseButtonDown = false
  private var leftMouseButtonCountdown = 0
  private var rightMouseButtonCountdown = 0

  private var isPaused: Boolean = false
  
  setUniforms()
  
  def onReloadedResources(): Unit = {
    setUniforms()
    world.columns.values.foreach(_.chunks.values.foreach(_.requestRenderUpdate()))
  }

  private def setUniforms(): Unit = {
    camera.setProjMatrix(blockShader)
    camera.setProjMatrix(blockSideShader)
    camera.setProjMatrix(selectedBlockShader)
    blockShader.setUniform1f("totalSize", world.totalSize)
    blockSideShader.setUniform1f("totalSize", world.totalSize)
    selectedBlockShader.setUniform1f("totalSize", world.totalSize)
    skyShader.setUniformMat4("invProjMatr", camera.invProjMatr)
    skyShader.setUniform2f("windowSize", Main.windowSize.x, Main.windowSize.y)
    crosshairShader.setUniform2f("windowSize", Main.windowSize.x, Main.windowSize.y)
  }

  def processKeys(key: Int, scancode: Int, action: Int, mods: Int): Boolean = {
    if (action == GLFW_PRESS) {
      if (key == GLFW_KEY_ESCAPE) {
        Main.pushScene(new PauseMenu(this))
        setPaused(true)
      } else if (key == GLFW_KEY_M) {
        playerInputHandler.moveWithMouse = !playerInputHandler.moveWithMouse
        setMouseCursorInvisible(playerInputHandler.moveWithMouse)
        Main.updateMousePos()
      } else if (key == GLFW_KEY_F) {
        playerInputHandler.player.flying = !playerInputHandler.player.flying
      } else if (key >= GLFW_KEY_1 && key <= GLFW_KEY_9) {
        playerInputHandler.player.selectedItemSlot = key - GLFW_KEY_1
      }
    }
    true
  }

  def processChar(character: Int): Boolean = true

  def setPaused(paused: Boolean): Unit = {
    if (paused != isPaused) {
      isPaused = paused

      setMouseCursorInvisible(!paused && playerInputHandler.moveWithMouse)
    }
  }

  private def setMouseCursorInvisible(invisible: Boolean): Unit = {
    glfwSetInputMode(Main.window, GLFW_CURSOR, if (invisible) GLFW_CURSOR_DISABLED else GLFW_CURSOR_NORMAL)
  }

  def processMouseButtons(button: Int, action: Int, mods: Int): Boolean = {
    if (button == 0) {
      rightMouseButtonDown = action != GLFW.GLFW_RELEASE
    } else if (button == 1) {
      leftMouseButtonDown = action != GLFW.GLFW_RELEASE
    }
    true
  }

  override def windowResized(width: Int, height: Int): Unit = {
    camera.aspect = width.toFloat / height
    camera.updateProjMatrix
    camera.setProjMatrix(blockShader)
    camera.setProjMatrix(blockSideShader)
    camera.setProjMatrix(selectedBlockShader)

    skyShader.setUniformMat4("invProjMatr", camera.invProjMatr)
    skyShader.setUniform2f("windowSize", width, height)
    crosshairShader.setUniform2f("windowSize", width, height)
  }
  
  override def windowTitle: String = 
         s"P: (${camera.position.x.toFloat}, ${camera.position.y.toFloat}, ${camera.position.z.toFloat}" + 
    s")    C: (${camera.blockCoords.x.toInt >> 4}, ${camera.blockCoords.y.toInt >> 4}, ${camera.blockCoords.z.toInt >> 4}" + 
    s")    R: (${math.toDegrees(camera.rotation.x).toFloat}, ${math.toDegrees(camera.rotation.y).toFloat}, ${math.toDegrees(camera.rotation.z).toFloat}" + 
    s")    v=${(100 * world.renderDistance).toInt / 100f}"

  def render(): Unit = {
    // render world etc.
    worldRenderer.render(camera)

    if (playerInputHandler.moveWithMouse) {
      crosshairShader.enable()
      crosshairRenderer.render()
    }
  }

  def tick(): Unit = {
    if (!isPaused) playerInputHandler.tick()
    camera.setPositionAndRotation(playerInputHandler.player)
    camera.updateCoords()
    camera.updateViewMatrix
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
              val skewCoords = BlockCoord(coords.x, coords.y, coords.z, world).toSkewCylCoord
              if (!HexBox.collides(blockType.bounds, skewCoords, playerInputHandler.player.bounds, CylCoord(camera.position, world))) {
                world.setBlock(new BlockState(coords, blockType))
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
          case Some((coords, side)) =>
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
  }

  def unload(): Unit = {
    world.unload()
    crosshairVAO.free()
    setMouseCursorInvisible(false)
  }
}
