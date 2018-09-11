package com.martomate.hexacraft.gui.location

import com.martomate.hexacraft.GameWindow

class LocationInfo16x9(_x: Float, _y: Float, _w: Float, _h: Float)(implicit window: GameWindow) extends LocationInfo {
  def x: Float = (_x * 2 - 1) * (16f / 9)
  def y: Float = _y * 2 - 1
  def w: Float = _w * 2 * 16 / 9// * (Main.aspectRatio / (16f / 9))
  def h: Float = _h * 2
}

object LocationInfo16x9 {
  def apply(_x: Float, _y: Float, _w: Float, _h: Float)(implicit window: GameWindow) =
    new LocationInfo16x9(_x, _y, _w, _h)
}
