package hexacraft.math.geometry

import munit.FunSuite
import org.joml.Vector2d

class SimplePolygonTest extends FunSuite {
  test("area is 1 for a unit square") {
    val p = SimplePolygon(IndexedSeq(Vector2d(2, 2), Vector2d(3, 2), Vector2d(3, 3), Vector2d(2, 3)))

    assertEqualsDouble(p.area, 1, 1e-9)
  }

  test("area is 1 for a tilted unit square") {
    val p = SimplePolygon(IndexedSeq(Vector2d(2, 2), Vector2d(3, 2), Vector2d(4, 3), Vector2d(3, 3)))

    assertEqualsDouble(p.area, 1, 1e-9)
  }

  test("area is 1 for an inverted unit square") {
    val p = SimplePolygon(IndexedSeq(Vector2d(2, 2), Vector2d(1, 2), Vector2d(1, 1), Vector2d(2, 1)))

    assertEqualsDouble(p.area, 1, 1e-9)
  }

  test("area is positive for both cw and ccw orientations") {
    val ccw = SimplePolygon(IndexedSeq(Vector2d(2, 2), Vector2d(3, 2), Vector2d(3, 3), Vector2d(2, 3)))
    val cw = SimplePolygon(IndexedSeq(Vector2d(2, 2), Vector2d(2, 3), Vector2d(3, 3), Vector2d(3, 2)))

    assertEqualsDouble(ccw.area, 1, 1e-9)
    assertEqualsDouble(cw.area, 1, 1e-9)
  }
}
