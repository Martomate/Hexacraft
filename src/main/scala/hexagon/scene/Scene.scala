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
