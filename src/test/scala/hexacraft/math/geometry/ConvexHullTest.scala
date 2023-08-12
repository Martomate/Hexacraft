package hexacraft.math.geometry

import munit.FunSuite
import org.joml.Vector2d

class ConvexHullTest extends FunSuite {
  test("four points in correct order works") {
    val polygon = ConvexHull.calculate(Seq(Vector2d(1, 0.1), Vector2d(1, 1), Vector2d(0.1, 1), Vector2d(0, 0)))

    val expected = IndexedSeq(Vector2d(0, 0), Vector2d(1, 0.1), Vector2d(1, 1), Vector2d(0.1, 1))
    assertEquals(polygon.points, expected)
  }

  test("four points in wrong order works") {
    val polygon = ConvexHull.calculate(Seq(Vector2d(1, 0.1), Vector2d(1, 1), Vector2d(0, 0), Vector2d(0.1, 1)))

    val expected = IndexedSeq(Vector2d(0, 0), Vector2d(1, 0.1), Vector2d(1, 1), Vector2d(0.1, 1))
    assertEquals(polygon.points, expected)
  }

  test("the polygon starts at the lowest y then the lowest x") {
    val polygon = ConvexHull.calculate(Seq(Vector2d(1, 0), Vector2d(1, 1), Vector2d(0, 1), Vector2d(0, 0)))

    val expected = IndexedSeq(Vector2d(0, 0), Vector2d(1, 0), Vector2d(1, 1), Vector2d(0, 1))
    assertEquals(polygon.points, expected)
  }

  test("extra internal points are removed") {
    val polygon =
      ConvexHull.calculate(Seq(Vector2d(1, 0.1), Vector2d(1, 1), Vector2d(0.4, 0.6), Vector2d(0.1, 1), Vector2d(0, 0)))

    val expected = IndexedSeq(Vector2d(0, 0), Vector2d(1, 0.1), Vector2d(1, 1), Vector2d(0.1, 1))
    assertEquals(polygon.points, expected)
  }

  test("multiple extra internal points are removed") {
    val polygon =
      ConvexHull.calculate(
        Seq(
          Vector2d(1, 0.1),
          Vector2d(1, 1),
          Vector2d(0.4, 0.6),
          Vector2d(0.1, 1),
          Vector2d(0.5, 0.6),
          Vector2d(0.4, 0.5),
          Vector2d(0, 0)
        )
      )

    val expected = IndexedSeq(Vector2d(0, 0), Vector2d(1, 0.1), Vector2d(1, 1), Vector2d(0.1, 1))
    assertEquals(polygon.points, expected)
  }
}
