package hexacraft.math.geometry

import org.joml.{Matrix2d, Vector2d}

import scala.collection.mutable.ArrayBuffer

object ConvexHull {

  /** Algorithm: Graham scan */
  def calculate(points: Seq[Vector2d]): SimplePolygon =
    val lowestPoint = points.minBy(v => (v.y, v.x))
    val sortedPoints = points.sortBy: p =>
      if p != lowestPoint then
        val v = p.sub(lowestPoint, new Vector2d)
        math.atan2(v.y, v.x)
      else -1

    val hull = ArrayBuffer.empty[Vector2d]
    hull += lowestPoint
    for p <- sortedPoints.tail do
      var i = hull.length - 1
      while i > 0 &&
        Matrix2d(
          hull(i - 1).sub(hull(i), new Vector2d),
          p.sub(hull(i), new Vector2d)
        ).determinant() > 0
      do
        hull -= hull(i)
        i -= 1
      hull += p

    SimplePolygon(hull.toIndexedSeq)
}
