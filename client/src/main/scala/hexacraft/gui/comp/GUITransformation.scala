package hexacraft.gui.comp

case class GUITransformation(x: Float, y: Float) {
  def offset(dx: Float, dy: Float): GUITransformation = GUITransformation(x + dx, y + dy)
}
