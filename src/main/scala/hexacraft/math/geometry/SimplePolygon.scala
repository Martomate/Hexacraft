package hexacraft.math.geometry

import org.joml.Vector2d

/** A polygon that does not intersect itself.
  * Note: the last point is automatically connected to the first one.
  */
class SimplePolygon(val points: IndexedSeq[Vector2d]) {
  def area: Double =
    var a = 0.0
    val root = points(0)
    var prev = points(1).sub(root, new Vector2d)

    for i <- 2 until points.length do
      val now = points(i).sub(root, new Vector2d)
      a += now.y * prev.x - now.x * prev.y
      prev = now

    math.abs(a / 2)
}
