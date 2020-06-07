package com.martomate.hexacraft.util

import org.scalatest.funsuite.AnyFunSuite

class MathUtilsTest extends AnyFunSuite {
  test("fitZ works correctly") {
    // make instance
    assert(MathUtils.fitZ(123.54, 200) == 123.54)
    //assert(CoordUtils.fitZ(10000123.54, 200) == 123.54)
    assert(MathUtils.fitZ(123.54-200, 200) == 123.54)

  }
}
