package com.martomate.hexacraft.scene

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.gui.{CharEvent, KeyEvent, MouseClickEvent, ScrollEvent}
import com.martomate.hexacraft.gui.comp.Component
import com.martomate.hexacraft.gui.location.LocationInfo16x9

abstract class Scene(implicit window: GameWindow) extends Component(LocationInfo16x9(0, 0, 1, 1)) {
  def windowResized(w: Int, h: Int): Unit = ()
  def framebufferResized(w: Int, h: Int): Unit = ()
  def windowTitle: String = ""

  def isOpaque: Boolean = true

  override def onMouseClickEvent(event: MouseClickEvent): Boolean = true
  override def onScrollEvent(event: ScrollEvent): Boolean = true
  override def onKeyEvent(event: KeyEvent): Boolean = true
  override def onCharEvent(event: CharEvent): Boolean = true
}
