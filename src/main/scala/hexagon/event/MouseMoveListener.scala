package hexagon.event

trait MouseMoveListener {
  def onMouseMoveEvent(x: Double, y: Double): Unit
}
