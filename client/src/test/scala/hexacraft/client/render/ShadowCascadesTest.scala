package hexacraft.client.render

import munit.FunSuite
import org.joml.{Matrix4f, Vector3d, Vector3f, Vector4f}

class ShadowCascadesTest extends FunSuite {
  private val sun = new Vector3f(0, 1, -1)
  private val projMatrix = new Matrix4f().perspective(1.2f, 16f / 9, 0.02f, 1000f)
  private val splits = IndexedSeq(10f, 40f, 140f)

  private def invViewMatrix(rotationX: Float, rotationY: Float): Matrix4f = {
    new Matrix4f().rotateX(rotationX).rotateY(rotationY).invert()
  }

  private def toShadowMap(cascade: ShadowCascades.Cascade, p: Vector3f): Vector3f = {
    val v = new Vector4f(p, 1).mul(cascade.matrix)
    new Vector3f(v.x, v.y, v.z).div(v.w)
  }

  /** The corners of the frustum slice between `near` and `far`, in the coordinates the world is rendered in */
  private def frustumSliceCorners(invView: Matrix4f, near: Float, far: Float): Seq[Vector3f] = {
    val tanX = 1 / projMatrix.m00()
    val tanY = 1 / projMatrix.m11()
    for {
      d <- Seq(near, far)
      sx <- Seq(-1, 1)
      sy <- Seq(-1, 1)
    } yield invView.transformPosition(new Vector3f(sx * tanX * d, sy * tanY * d, -d))
  }

  test("each cascade contains its frustum slice") {
    for (rx, ry) <- Seq((0f, 0f), (0.5f, 1f), (-1.2f, 2.5f), (1.5f, -0.3f)) do {
      val invView = invViewMatrix(rx, ry)
      val cascades = ShadowCascades.compute(invView, projMatrix, new Vector3d(12.3, 45.6, 78.9), sun, splits, 1024)

      for i <- splits.indices do {
        val near = if i == 0 then 0.02f else splits(i - 1)
        for corner <- frustumSliceCorners(invView, near, splits(i)) do {
          val p = toShadowMap(cascades(i), corner)
          assert(math.abs(p.x) <= 1 && math.abs(p.y) <= 1 && math.abs(p.z) <= 1, s"cascade $i, rotation ($rx, $ry): $p")
        }
      }
    }
  }

  test("the size of the cascades does not depend on the camera rotation") {
    val camPos = new Vector3d(1, 2, 3)
    val a = ShadowCascades.compute(invViewMatrix(0, 0), projMatrix, camPos, sun, splits, 1024)
    val b = ShadowCascades.compute(invViewMatrix(0.7f, 2f), projMatrix, camPos, sun, splits, 1024)

    for i <- splits.indices do {
      assertEqualsFloat(a(i).texelSize, b(i).texelSize, 1e-6f)
    }
  }

  test("the texel grid stays fixed in the world when the camera moves") {
    val invView = invViewMatrix(0.3f, 0.4f)
    val camA = new Vector3d(100.0, 20.0, 300.0)
    val camB = new Vector3d(100.37, 20.11, 299.71)
    val a = ShadowCascades.compute(invView, projMatrix, camA, sun, splits, 1024)
    val b = ShadowCascades.compute(invView, projMatrix, camB, sun, splits, 1024)

    // A point that is fixed in the world, but expressed relative to each camera
    val worldPoint = new Vector3d(102.0, 21.0, 296.0)
    val relA = new Vector3f(worldPoint.sub(camA, new Vector3d))
    val relB = new Vector3f(worldPoint.sub(camB, new Vector3d))

    for i <- splits.indices do {
      val texelsA = toShadowMap(a(i), relA).mul(512) // 512 = half the resolution
      val texelsB = toShadowMap(b(i), relB).mul(512)

      // the point may end up in another texel, but at the same position within the texel
      val fracDiffX = (texelsA.x - texelsB.x) - math.round(texelsA.x - texelsB.x)
      val fracDiffY = (texelsA.y - texelsB.y) - math.round(texelsA.y - texelsB.y)
      assertEqualsFloat(fracDiffX, 0, 1e-2f)
      assertEqualsFloat(fracDiffY, 0, 1e-2f)
    }
  }

  test("boundingSphere contains both the near and far corners") {
    val diagonalSq = 0.9
    for (near, far) <- Seq((0.0, 10.0), (10.0, 40.0), (40.0, 140.0), (1.0, 1.5)) do {
      val (center, radius) = ShadowCascades.boundingSphere(near, far, diagonalSq)
      val nearCornerDist = math.sqrt((center - near) * (center - near) + near * near * diagonalSq)
      val farCornerDist = math.sqrt((far - center) * (far - center) + far * far * diagonalSq)
      assert(nearCornerDist <= radius + 1e-9)
      assert(farCornerDist <= radius + 1e-9)
    }
  }
}
