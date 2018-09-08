package com.martomate.hexacraft.gui.location

import com.martomate.hexacraft.Main

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
