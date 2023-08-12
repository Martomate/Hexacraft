package hexacraft.util

import munit.FunSuite

class MathUtilsTest extends FunSuite {
  test("fitZ should work correctly") {
    assertEquals(MathUtils.fitZ(123.54, 200), 123.54)
    assertEquals(MathUtils.fitZ(123.54 - 200, 200), 123.54)
  }

  test("smoothstep maps 0 to 0") {
    assertEqualsDouble(MathUtils.smoothstep(0.0), 0.0, 1e-12)
  }

  test("smoothstep maps 1 to 1") {
    assertEqualsDouble(MathUtils.smoothstep(1.0), 1.0, 1e-12)
  }

  test("smoothstep maps -1 to 0") {
    assertEqualsDouble(MathUtils.smoothstep(-1.0), 0.0, 1e-12)
  }

  test("smoothstep maps 2 to 1") {
    assertEqualsDouble(MathUtils.smoothstep(2.0), 1.0, 1e-12)
  }

  test("smoothstep maps 0.5 to 0.5") {
    assertEqualsDouble(MathUtils.smoothstep(0.5), 0.5, 1e-12)
  }

  test("smoothstep maps 0.25 to lower than 0.25") {
    assert(MathUtils.smoothstep(0.25) < 0.25)
  }

  test("smoothstep maps 0.75 to higher than 0.75") {
    assert(MathUtils.smoothstep(0.75) > 0.75)
  }
}
