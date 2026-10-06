package hexacraft.world

import munit.FunSuite
import org.joml.Vector3d

class PointHexagonTest extends FunSuite {
  private val Eps = 1e-9

  /** A hexagonal prism with the given center (of its base) whose corners are at angles 0, 60, 120, ... */
  private def hexagon(center: Vector3d, radius: Double = 1, height: Double = 1): PointHexagon = {
    def ring(y: Double) = Array.tabulate(6) { i =>
      val angle = i * Math.PI / 3
      Vector3d(center.x + radius * Math.cos(angle), center.y + y, center.z + radius * Math.sin(angle))
    }
    new PointHexagon(ring(height), ring(0))
  }

  private def ray(x: Double, y: Double, z: Double) = Ray(Vector3d(x, y, z).normalize())

  private def assertDistance(actual: Option[Double], expected: Double)(using munit.Location): Unit = {
    assert(actual.isDefined, "expected a hit but got None")
    assertEqualsDouble(actual.get, expected, Eps)
  }

  test("a ray going straight down hits the top face first") {
    val hex = hexagon(Vector3d(0, -3, 0)) // spans y = -3 to -2

    assertDistance(hex.distanceToFace(ray(0, -1, 0), BlockFace.Top), 2)
    assertDistance(hex.distanceToFace(ray(0, -1, 0), BlockFace.Bottom), 3)
    assertDistance(hex.distanceToBox(ray(0, -1, 0)), 2)
  }

  test("a ray going straight up hits the bottom face first") {
    val hex = hexagon(Vector3d(0, 2, 0)) // spans y = 2 to 3

    assertDistance(hex.distanceToFace(ray(0, 1, 0), BlockFace.Bottom), 2)
    assertDistance(hex.distanceToFace(ray(0, 1, 0), BlockFace.Top), 3)
    assertDistance(hex.distanceToBox(ray(0, 1, 0)), 2)
  }

  test("a tilted ray gives the distance along the ray, not the vertical distance") {
    val hex = hexagon(Vector3d(0, -3, 0))
    val tilted = Vector3d(0.2, -1, 0)

    val dir = Vector3d(tilted).normalize()
    val expectedDistance = 2 * tilted.length()

    // the ray reaches y = -2 after going 2 units down, which is 2 * |(0.2, -1, 0)| along the ray
    assertDistance(hex.distanceToBox(Ray(dir)), expectedDistance)
  }

  test("a ray hits the side face it passes through") {
    // the ray enters between the corners at 120 and 180 degrees (side 2), at the height of half the hexagon
    val hex = hexagon(Vector3d(5, -0.5, -0.3))

    val expectedX = 5 - 1 + 0.3 / Math.sqrt(3) // the edge from (-0.5, 0.866) to (-1, 0) in the hexagon
    assertDistance(hex.distanceToFace(ray(1, 0, 0), BlockFace.Side(2)), expectedX)
    assertDistance(hex.distanceToBox(ray(1, 0, 0)), expectedX)

    for side <- 0 until 6 if side != 2 do {
      hex.distanceToFace(ray(1, 0, 0), BlockFace.Side(side)) match {
        case Some(distance) =>
          assert(distance > expectedX - Eps)
        case _ =>
      }
    }
  }

  test("a ray that misses the hexagon gives None") {
    val hex = hexagon(Vector3d(5, -3, 0))

    for i <- 0 until 8 do {
      assertEquals(hex.distanceToFace(ray(0, -1, 0), BlockFace.fromInt(i)), None)
    }
    assertEquals(hex.distanceToBox(ray(0, -1, 0)), None)
  }

  test("a hexagon behind the ray gives None") {
    val hex = hexagon(Vector3d(0, 2, 0))

    assertEquals(hex.distanceToBox(ray(0, -1, 0)), None)
  }
}
