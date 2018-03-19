package hexacraft.gui.comp

import hexacraft.Main

case class LocationInfo(x: Float, y: Float, w: Float, h: Float) {
  def containsPoint(xpos: Float, ypos: Float): Boolean = xpos >= x && xpos <= x + w && ypos >= y && ypos <= y + h
  def containsPoint(pos: (Float, Float)): Boolean = pos._1 >= x && pos._1 <= x + w && pos._2 >= y && pos._2 <= y + h
  def containsMouse(offset: (Float, Float)): Boolean = {
    containsPoint((Main.normalizedMousePos.x * Main.aspectRatio + 1) * 0.5f - offset._1, (Main.normalizedMousePos.y + 1) * 0.5f - offset._2)
  }

  def inScreenCoordinates: (Int, Int, Int, Int) = {
    ((x * Main.windowSize.x).toInt,
     (y * Main.windowSize.y).toInt,
     (w * Main.windowSize.x).toInt,
     (h * Main.windowSize.y).toInt)
  }
}
