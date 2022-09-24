package com.martomate.hexacraft.scene

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.gui.{CharEvent, KeyEvent, LocationInfo16x9, MouseClickEvent, ScrollEvent}
import com.martomate.hexacraft.gui.comp.Component

abstract class Scene(implicit window: GameWindow) extends Component {
  def windowResized(w: Int, h: Int): Unit = ()
  def framebufferResized(w: Int, h: Int): Unit = ()
  def windowTitle: String = ""

  def isOpaque: Boolean = true

  override def onMouseClickEvent(event: MouseClickEvent): Boolean = true
  override def onScrollEvent(event: ScrollEvent): Boolean = true
  override def onKeyEvent(event: KeyEvent): Boolean = true
  override def onCharEvent(event: CharEvent): Boolean = true
}
