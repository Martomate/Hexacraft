package com.martomate.hexacraft.event

trait MouseMoveListener {
  def onMouseMoveEvent(event: MouseMoveEvent): Unit
}
case class MouseMoveEvent(x: Double, y: Double)
case class ScrollEvent(xoffset: Float, yoffset: Float)
