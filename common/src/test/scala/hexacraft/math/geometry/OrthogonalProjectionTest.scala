package hexacraft.math.geometry

import munit.FunSuite
import org.joml.Vector3d

class OrthogonalProjectionTest extends FunSuite {
  test("axes are unit vectors") {
    val p = OrthogonalProjection.inDirection(Vector3d(1, 2, 3))
    val (x, y) = p.axes

    assertEqualsDouble(x.length(), 1, 1e-9)
    assertEqualsDouble(y.length(), 1, 1e-9)
  }

  test("axes are orthogonal to each other") {
    val p = OrthogonalProjection.inDirection(Vector3d(1, 2, 3))
    val (x, y) = p.axes

    assertEqualsDouble(x.dot(y), 0, 1e-9)
  }

  test("axes are orthogonal to the direction vector") {
    val dir = Vector3d(1, 2, 3)
    val p = OrthogonalProjection.inDirection(dir)
    val (x, y) = p.axes

    assertEqualsDouble(x.dot(dir), 0, 1e-9)
    assertEqualsDouble(y.dot(dir), 0, 1e-9)
  }

  test("the direction vector projects to 0") {
    val dir = Vector3d(1, 2, 3)
    val p = OrthogonalProjection.inDirection(dir)
    val v = p.project(dir)

    assertEqualsDouble(v.x, 0, 1e-9)
    assertEqualsDouble(v.y, 0, 1e-9)
  }

  test("the x-axis projects to (1, 0)") {
    val p = OrthogonalProjection.inDirection(Vector3d(1, 2, 3))
    val (x, _) = p.axes
    val v = p.project(x)

    assertEqualsDouble(v.x, 1, 1e-9)
    assertEqualsDouble(v.y, 0, 1e-9)
  }

  test("the y-axis projects to (0, 1)") {
    val p = OrthogonalProjection.inDirection(Vector3d(1, 2, 3))
    val (_, y) = p.axes
    val v = p.project(y)

    assertEqualsDouble(v.x, 0, 1e-9)
    assertEqualsDouble(v.y, 1, 1e-9)
  }

  test("the reverse x-axis projects to (-1, 0)") {
    val p = OrthogonalProjection.inDirection(Vector3d(1, 2, 3))
    val (x, _) = p.axes
    val v = p.project(x.mul(-1, new Vector3d))

    assertEqualsDouble(v.x, -1, 1e-9)
    assertEqualsDouble(v.y, 0, 1e-9)
  }

  test("the reverse y-axis projects to (0, -1)") {
    val p = OrthogonalProjection.inDirection(Vector3d(1, 2, 3))
    val (_, y) = p.axes
    val v = p.project(y.mul(-1, new Vector3d))

    assertEqualsDouble(v.x, 0, 1e-9)
    assertEqualsDouble(v.y, -1, 1e-9)
  }

  test("a direction vector of 0 throws an exception") {
    intercept[IllegalArgumentException](OrthogonalProjection.inDirection(Vector3d(0, 0, 0)))
  }
}
