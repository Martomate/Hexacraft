package com.martomate.hexacraft.gui

sealed trait Event

case class KeyEvent(key: Int, scancode: Int, action: Int, mods: Int) extends Event

case class CharEvent(character: Int) extends Event

case class MouseClickEvent(button: Int, action: Int, mods: Int, mousePos: (Float, Float)) extends Event {
  def withMouseTranslation(dx: Float, dy: Float): MouseClickEvent = copy(mousePos = (mousePos._1 + dx, mousePos._2 + dy))
}

case class MouseMoveEvent(x: Double, y: Double) extends Event

case class ScrollEvent(xoffset: Float, yoffset: Float) extends Event

case class WindowResizeEvent(w: Double, h: Double) extends Event
