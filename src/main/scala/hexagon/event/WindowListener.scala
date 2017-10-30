package hexagon.event

trait WindowListener {
  def onWindowResized(w: Double, h: Double): Unit
}
