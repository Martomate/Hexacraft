package hexacraft.gui

import hexacraft.gui.comp.Component

abstract class Scene extends Component {
  def windowResized(w: Int, h: Int): Unit = ()
  def framebufferResized(w: Int, h: Int): Unit = ()

  def isOpaque: Boolean = true

  override def handleEvent(event: Event): Boolean = true
}
