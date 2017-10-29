package hexagon.event

trait MouseClickListener {
  def onMouseClickEvent(button: Int, action: Int, mods: Int): Unit
}
