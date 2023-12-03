package hexacraft.math.geometry

import org.joml.{Matrix3d, Vector2d, Vector3d, Vector3dc}

class OrthogonalProjection private (m: Matrix3d) {
  def project(v: Vector3dc): Vector2d =
    val p = v.mul(m, new Vector3d)
    Vector2d(p.x, p.y)

  def axes: (Vector3d, Vector3d) =
    val inv = m.invert(new Matrix3d)
    val xAxis = Vector3d(1, 0, 0).mul(inv)
    val yAxis = Vector3d(0, 1, 0).mul(inv)
    (xAxis, yAxis)
}

object OrthogonalProjection {
  private val v1: Vector3dc = Vector3d(1, 0, 0)
  private val v2: Vector3dc = Vector3d(0, 1, 0)

  private def unitVectorDifferentFrom(v: Vector3dc): Vector3dc =
    if math.abs(v1.dot(v)) < math.abs(v2.dot(v)) then v1 else v2

  def inDirection(dir: Vector3dc): OrthogonalProjection =
    require(dir.lengthSquared > 0) // fail fast instead of producing NaN results

    val up = unitVectorDifferentFrom(dir)
    val m = new Matrix3d().setLookAlong(dir, up)
    new OrthogonalProjection(m)
}
