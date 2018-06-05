package hexacraft.gui.comp

import hexacraft.Main

/** x, y, w, h are position and size where the screen has center (0, 0), width aspectRatio * 2 and height 2 */
abstract class LocationInfo {
  def x: Float
  def y: Float
  def w: Float
  def h: Float

  def containsPoint(px: Float, py: Float): Boolean = px >= x && px <= x + w && py >= y && py <= y + h
  final def containsPoint(pos: (Float, Float)): Boolean = containsPoint(pos._1, pos._2)

  def containsMouse(offset: (Float, Float)): Boolean = {
    containsPoint(Main.normalizedMousePos.x * Main.aspectRatio - offset._1, Main.normalizedMousePos.y - offset._2)
  }

  def inScreenCoordinates: (Int, Int, Int, Int) = (
      ((x + Main.aspectRatio) * 0.5f * Main.windowSize.y).round,
      ((y + 1) * 0.5f * Main.windowSize.y).round,
      (w * 0.5f * Main.windowSize.y).round,
      (h * 0.5f * Main.windowSize.y).round
  )
}

object LocationInfo {
  def apply(_x: Float, _y: Float, _w: Float, _h: Float) = new LocationInfo16x9(_x, _y, _w, _h)
}

class LocationInfoIdentity(val x: Float, val y: Float, val w: Float, val h: Float) extends LocationInfo

class LocationInfo16x9(_x: Float, _y: Float, _w: Float, _h: Float) extends LocationInfo {
  def x: Float = (_x * 2 - 1) * (16f / 9)
  def y: Float = _y * 2 - 1
  def w: Float = _w * 2 * 16 / 9// * (Main.aspectRatio / (16f / 9))
  def h: Float = _h * 2
}
