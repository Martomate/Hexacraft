package com.martomate.hexacraft.gui

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.gui.comp.Component

abstract class Scene(implicit window: GameWindow) extends Component {
  def windowResized(w: Int, h: Int): Unit = ()
  def framebufferResized(w: Int, h: Int): Unit = ()
  def windowTitle: String = ""

  def isOpaque: Boolean = true

  override def onMouseClickEvent(event: Event.MouseClickEvent): Boolean = true
  override def onScrollEvent(event: Event.ScrollEvent): Boolean = true
  override def onKeyEvent(event: Event.KeyEvent): Boolean = true
  override def onCharEvent(event: Event.CharEvent): Boolean = true
}
