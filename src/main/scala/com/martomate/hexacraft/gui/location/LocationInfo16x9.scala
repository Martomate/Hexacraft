package com.martomate.hexacraft.gui.location

class LocationInfo16x9(_x: Float, _y: Float, _w: Float, _h: Float) extends LocationInfo {
  def x: Float = (_x * 2 - 1) * (16f / 9)
  def y: Float = _y * 2 - 1
  def w: Float = _w * 2 * 16 / 9// * (Main.aspectRatio / (16f / 9))
  def h: Float = _h * 2
}
