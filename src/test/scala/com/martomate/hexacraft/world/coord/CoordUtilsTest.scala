package com.martomate.hexacraft.world.coord

import org.scalatest.FunSuite

class CoordUtilsTest extends FunSuite {
  test("fitZ works correctly") {
    // make instance
    assert(CoordUtils.fitZ(123.54, 200) == 123.54)
    //assert(CoordUtils.fitZ(10000123.54, 200) == 123.54)
    assert(CoordUtils.fitZ(123.54-200, 200) == 123.54)

  }
}
