package com.martomate.hexacraft.world.gen.noise

import org.scalatest.FunSuite

class NoiseInterpolator2DTest extends FunSuite {
  test("same input should give same output") {
    val n = new NoiseInterpolator2D(4, 4, (i, j) => i + 2 * j)
    for (i <- 0 until 16; j <- 0 until 16) {
      assert(n(i, j) == n(i, j))
    }
  }

  test("input should give correct output") {
    val func: (Int, Int) => Double = (i, j) => i + 2 * j
    val n = new NoiseInterpolator2D(4, 4, func)
    for (i <- 0 until 4; j <- 0 until 4) {
      assertResult(func(i, j))(n(4 * i, 4 * j))
    }
  }
}
