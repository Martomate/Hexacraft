package hexacraft.event

trait WindowListener {
  def onWindowResized(event: WindowResizeEvent): Unit
}
case class WindowResizeEvent(w: Double, h: Double)
