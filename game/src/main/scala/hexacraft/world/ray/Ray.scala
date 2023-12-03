package hexacraft.world.ray

import hexacraft.world.camera.Camera

import org.joml.{Vector2fc, Vector3d, Vector4f}

class Ray(val v: Vector3d):

  /** @return
    *   true if the ray goes to the right of the line from `up` to `down` in a reference frame where
    *   `up` is directly above `down` (i.e. possibly rotated)
    */
  def toTheRight(down: Vector3d, up: Vector3d): Boolean = down.dot(up.cross(v, new Vector3d)) <= 0

  def intersectsPolygon(points: PointHexagon, side: Int): Boolean =
    val rightSeq =
      if side < 2 then
        for index <- 0 until 6
        yield
          val PA = if side == 0 then points.up(index) else points.down(index)
          val PB = if side == 0 then points.up((index + 1) % 6) else points.down((index + 1) % 6)

          this.toTheRight(PA, PB)
      else
        val order = Seq(0, 1, 3, 2)
        for index <- 0 until 4
        yield
          val aIdx = (order(index) % 2 + side - 2) % 6
          val PA = if order(index) / 2 == 0 then points.up(aIdx) else points.down(aIdx)

          val bIdx = (order((index + 1) % 4) % 2 + side - 2) % 6
          val PB = if order((index + 1) % 4) / 2 == 0 then points.up(bIdx) else points.down(bIdx)

          this.toTheRight(PA, PB)
    allElementsSame(rightSeq)

  private def allElementsSame(seq: IndexedSeq[Boolean]) = !seq.exists(_ != seq(0))

object Ray:
  def fromScreen(camera: Camera, normalizedScreenCoords: Vector2fc): Option[Ray] =
    val coords = normalizedScreenCoords
    if coords.x < -1 || coords.x > 1 || coords.y < -1 || coords.y > 1
    then None
    else
      val coords4 = new Vector4f(coords.x, coords.y, -1, 1)
      coords4.mul(camera.proj.invMatrix)
      coords4.set(coords4.x, coords4.y, -1, 0)
      coords4.mul(camera.view.invMatrix)
      val ray = new Vector3d(coords4.x, coords4.y, coords4.z)
      ray.normalize()
      Some(Ray(ray))
