package hexagon.gui.comp

import hexagon.Main

case class LocationInfo(x: Float, y: Float, w: Float, h: Float) {
  def containsPoint(xpos: Float, ypos: Float): Boolean = xpos >= x && xpos <= x + w && ypos >= y && ypos <= y + h
  def containsMouse: Boolean = containsPoint((Main.normalizedMousePos.x + 1) * 0.5f, (Main.normalizedMousePos.y + 1) * 0.5f)
}
