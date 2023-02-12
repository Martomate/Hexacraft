package com.martomate.hexacraft.util

import munit.FunSuite

class MathUtilsTest extends FunSuite {
  test("fitZ should work correctly") {
    assertEquals(MathUtils.fitZ(123.54, 200), 123.54)
    assertEquals(MathUtils.fitZ(123.54 - 200, 200), 123.54)
  }
}
