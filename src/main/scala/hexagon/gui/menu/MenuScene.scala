package hexagon.gui.menu

import hexagon.gui.comp.{Component, LocationInfo}
import hexagon.resource.TextureSingle
import hexagon.scene.Scene

import scala.collection.mutable.ArrayBuffer

abstract class MenuScene extends Scene {
  private val components: ArrayBuffer[Component] = ArrayBuffer.empty[Component]

  protected def addComponent(comp: Component): Unit = components.append(comp)

  protected var hasDefaultBackground: Boolean = true

  override def render(): Unit = {
    if (hasDefaultBackground) Component.drawImage(MenuScene.entireBackground, TextureSingle.getTexture("textures/gui/menu/background"))
    components.foreach(_.render())
  }

  override def tick(): Unit = components.foreach(_.tick())

  def unload(): Unit = ()

  def processKeys(key: Int, scancode: Int, action: Int, mods: Int): Boolean = {
    components.foreach(_.onKeyEvent(key, scancode, action, mods))
    true
  }

  def processChar(character: Int): Boolean = {
    components.foreach(_.onCharEvent(character))
    true
  }

  def processMouseButtons(button: Int, action: Int, mods: Int): Boolean = {
    components.foreach(_.onMouseClickEvent(button, action, mods))
    true
  }
}

object MenuScene {
  val entireBackground: LocationInfo = LocationInfo(0, 0, 1, 1)
}