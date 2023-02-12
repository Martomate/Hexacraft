package com.martomate.hexacraft.world.gen.noise

import munit.FunSuite

class NoiseInterpolator2DTest extends FunSuite {
  test("noise should be the same for the same input") {
    val n = new NoiseInterpolator2D(4, 4, (i, j) => i + 2 * j)
    for (i <- 0 until 16; j <- 0 until 16) {
      assert(n(i, j) == n(i, j))
    }
  }

  test("noise should be correct") {
    val func: (Int, Int) => Double = (i, j) => i + 2 * j
    val n = new NoiseInterpolator2D(4, 4, func)
    for (i <- 0 until 4; j <- 0 until 4) {
      assertEquals(n(4 * i, 4 * j), func(i, j))
    }
  }
}
