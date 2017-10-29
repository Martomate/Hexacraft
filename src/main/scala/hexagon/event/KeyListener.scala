package hexagon.event

trait KeyListener {
  def onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Unit
}
