package hexacraft.gui

import hexacraft.gui.comp.Component

abstract class Scene extends Component {
  def windowFocusChanged(focused: Boolean): Unit = ()
  def windowResized(w: Int, h: Int): Unit = ()
  def frameBufferResized(w: Int, h: Int): Unit = ()

  def isOpaque: Boolean = true

  override def handleEvent(event: Event): Boolean = true
}
