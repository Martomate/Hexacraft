package hexacraft.event

trait MouseClickListener {
  def onMouseClickEvent(event: MouseClickEvent): Unit
}
case class MouseClickEvent(button: Int, action: Int, mods: Int, mousePos: (Float, Float)) {
  def withMouseTranslation(dx: Float, dy: Float): MouseClickEvent = copy(mousePos = (mousePos._1 + dx, mousePos._2 + dy))
}
