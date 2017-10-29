package hexagon.gui.menu

import hexagon.Main
import hexagon.gui.comp.Component
import hexagon.resource.Shader
import hexagon.scene.{GameScene, Scene}
import org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER

import scala.collection.mutable.ArrayBuffer

abstract class MenuScene extends Scene {
  protected val imageShader: Shader = Shader.get("image").get

  private val components: ArrayBuffer[Component] = ArrayBuffer.empty[Component]

  protected def addComponent(comp: Component): Unit = components.append(comp)

  override def render(): Unit = {
    components.foreach(_.render())
  }

  override def tick(): Unit = ()

  def unload(): Unit = ()

  def processKeys(key: Int, scancode: Int, action: Int, mods: Int): Boolean = {
    components.foreach(_.onKeyEvent(key, scancode, action, mods))
    true
  }

  def processMouseButtons(button: Int, action: Int, mods: Int): Boolean = {
    components.foreach(_.onMouseClickEvent(button, action, mods))
    true
  }
}
