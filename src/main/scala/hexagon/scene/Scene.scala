package hexagon.scene

import org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
import org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
import org.lwjgl.glfw.GLFW.GLFW_PRESS

import hexagon.resource.Shader
import hexagon.Main

trait Scene {
  /** Returns true to consume the event */
  def processKeys(key: Int, scancode: Int, action: Int, mods: Int): Boolean
  def processMouseButtons(button: Int, action: Int, mods: Int): Boolean
  def windowResized(w: Int, h: Int): Unit = {}
  def render(): Unit
  def tick(): Unit
  def unload(): Unit
  def onReloadedResources(): Unit
  def windowTitle: String = ""
}

abstract class MenuScene extends Scene {
  val guiShader = Shader.getShader("gui")

  def unload(): Unit = {

  }
}

class MainMenuScene extends MenuScene {
  def processKeys(key: Int, scancode: Int, action: Int, mods: Int): Boolean = {
    if (action == GLFW_PRESS) key match {
      case GLFW_KEY_ENTER => Main.pushScene(new GameScene)
      case _              =>
    }
    true
  }

  def processMouseButtons(button: Int, action: Int, mods: Int): Boolean = {
    false
  }

  override def render(): Unit = {

  }

  override def tick(): Unit = {
  }
  
  def onReloadedResources(): Unit = ()
}

class PauseMenuScene extends MenuScene {
  def processKeys(key: Int, scancode: Int, action: Int, mods: Int): Boolean = {
    if (action == GLFW_PRESS) key match {
      case GLFW_KEY_ESCAPE => Main.popScene
      case _               =>
    }
    true
  }

  def processMouseButtons(button: Int, action: Int, mods: Int): Boolean = {
    false
  }

  override def render(): Unit = {

  }

  override def tick(): Unit = {
  }
  
  def onReloadedResources(): Unit = ()
}
