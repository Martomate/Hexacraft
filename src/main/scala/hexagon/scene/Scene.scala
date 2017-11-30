package hexagon.scene

import hexagon.event._
import hexagon.gui.comp.{Component, LocationInfo}

abstract class Scene extends Component(LocationInfo(0, 0, 1, 1)) {
  def windowResized(w: Int, h: Int): Unit = ()
  def windowTitle: String = ""

  override def onMouseMoveEvent(event: MouseMoveEvent): Boolean = true
  override def onMouseClickEvent(event: MouseClickEvent): Boolean = true
  override def onScrollEvent(event: ScrollEvent): Boolean = true
  override def onKeyEvent(event: KeyEvent): Boolean = true
  override def onCharEvent(event: CharEvent): Boolean = true
}
