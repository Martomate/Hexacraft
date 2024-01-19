package hexacraft.gui

import org.joml.Vector2ic

/** x, y, w, h are position and size. The entire screen corresponds to `(-a, -1, 2 * a, 2)` where
  * `a` is `window.aspectRatio`.
  *
  * If `x == 0` and `y == 0` the rectangle will always start at the center of the screen, and if `w == h`
  * the rectangle will always look like a square on the screen.
  */
case class LocationInfo(x: Float, y: Float, w: Float, h: Float) {
  def containsPoint(px: Float, py: Float): Boolean = {
    px >= x && px <= x + w && py >= y && py <= y + h
  }

  final def containsPoint(pos: (Float, Float)): Boolean = {
    containsPoint(pos._1, pos._2)
  }

  def inScaledScreenCoordinates(frameBufferSize: Vector2ic): FrameBufferRectangle = {
    val aspectRatio = frameBufferSize.x.toFloat / frameBufferSize.y
    FrameBufferRectangle(
      ((x + aspectRatio) * 0.5f * frameBufferSize.y).round,
      ((y + 1) * 0.5f * frameBufferSize.y).round,
      (w * 0.5f * frameBufferSize.y).round,
      (h * 0.5f * frameBufferSize.y).round
    )
  }

  def expand(d: Float): LocationInfo = {
    LocationInfo(x - d, y - d, w + 2 * d, h + 2 * d)
  }
}

object LocationInfo {
  def from16x9(x: Float, y: Float, w: Float, h: Float): LocationInfo = {
    LocationInfo((x * 2 - 1) * (16f / 9), y * 2 - 1, w * 2 * 16 / 9, h * 2)
  }

  /** @return the smallest enclosing rectangle */
  def hull(r1: LocationInfo, r2: LocationInfo): LocationInfo = {
    val minX = math.min(r1.x, r2.x)
    val minY = math.min(r1.y, r2.y)
    val maxX = math.max(r1.x + r1.w, r2.x + r2.w)
    val maxY = math.max(r1.y + r1.h, r2.y + r2.h)

    LocationInfo(minX, minY, maxX - minX, maxY - minY)
  }
}

case class FrameBufferRectangle(x: Int, y: Int, w: Int, h: Int)
