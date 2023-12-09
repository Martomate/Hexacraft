package hexacraft.world.coord.fp

import hexacraft.world.CylinderSize

import munit.FunSuite

class CylCoordsTest extends FunSuite {
  given CylinderSize = CylinderSize(3)
  val eps = 1e-9

  test("distanceSq should be 0 for itself") {
    val a = CylCoords(5, 2, 8)
    val b = CylCoords(5, 2, 8)
    assertEquals(a.distanceSq(b), 0.0)
  }
  test("distanceSq should work for one axis without wrap") {
    assertEqualsDouble(CylCoords(5, 2, 8) distanceSq CylCoords(5 + 2.3, 2, 8), 2.3 * 2.3, eps)
    assertEqualsDouble(CylCoords(5, 2, 8) distanceSq CylCoords(5 - 2.3, 2, 8), 2.3 * 2.3, eps)
    assertEqualsDouble(CylCoords(5, 2, 8) distanceSq CylCoords(5, 2 + 2.3, 8), 2.3 * 2.3, eps)
    assertEqualsDouble(CylCoords(5, 2, 8) distanceSq CylCoords(5, 2 - 2.3, 8), 2.3 * 2.3, eps)
    assertEqualsDouble(CylCoords(5, 2, 8) distanceSq CylCoords(5, 2, 8 + 2.3), 2.3 * 2.3, eps)
    assertEqualsDouble(CylCoords(5, 2, 8) distanceSq CylCoords(5, 2, 8 - 2.3), 2.3 * 2.3, eps)
  }
  test("distanceSq should work for one axis with wrap") {
    assertEqualsDouble(CylCoords(5, 2, 1 - 2.3) distanceSq CylCoords(5, 2, 1), 2.3 * 2.3, eps)
    assertEqualsDouble(CylCoords(5, 2, 1) distanceSq CylCoords(5, 2, 1 - 2.3), 2.3 * 2.3, eps)
  }
  test("distanceSq should work for three axes without wrap") {
    assertEqualsDouble(
      CylCoords(5, 2, 8) distanceSq CylCoords(7.3, 2.1, 8 - 2.9),
      2.3 * 2.3 + 0.1 * 0.1 + 2.9 * 2.9,
      eps
    )
  }
  test("distanceSq should work for three axes with wrap") {
    assertEqualsDouble(
      CylCoords(5, 2, 1) distanceSq CylCoords(7.3, 2.1, 1 - 2.9),
      2.3 * 2.3 + 0.1 * 0.1 + 2.9 * 2.9,
      eps
    )
  }
}
