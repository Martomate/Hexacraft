package com.martomate.hexacraft.gui.location

import com.martomate.hexacraft.GameWindow

/** x, y, w, h are position and size where the screen has center (0, 0), width aspectRatio * 2 and height 2 */
abstract class LocationInfo(implicit window: GameWindow) {
  def x: Float
  def y: Float
  def w: Float
  def h: Float

  def containsPoint(px: Float, py: Float): Boolean = px >= x && px <= x + w && py >= y && py <= y + h
  final def containsPoint(pos: (Float, Float)): Boolean = containsPoint(pos._1, pos._2)

  def containsMouse(xOff: Float, yOff: Float): Boolean = containsMouse((xOff, yOff))

  def containsMouse(offset: (Float, Float)): Boolean = {
    containsPoint(window.normalizedMousePos.x * window.aspectRatio - offset._1, window.normalizedMousePos.y - offset._2)
  }

  def inScreenCoordinates: (Int, Int, Int, Int) = (
      ((x + window.aspectRatio) * 0.5f * window.windowSize.y).round,
      ((y + 1) * 0.5f * window.windowSize.y).round,
      (w * 0.5f * window.windowSize.y).round,
      (h * 0.5f * window.windowSize.y).round)

  def inScaledScreenCoordinates: (Int, Int, Int, Int) = {
    val (xx, yy, ww, hh) = this.inScreenCoordinates
    val sx = window.pixelScale.x()
    val sy = window.pixelScale.y()
    (xx * sx, yy * sy, ww * sx, hh * sy)
  }
}
