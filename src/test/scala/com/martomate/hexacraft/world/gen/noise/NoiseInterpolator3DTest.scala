package com.martomate.hexacraft.world.gen.noise

import org.scalatest.FunSuite

class NoiseInterpolator3DTest extends FunSuite {
  test("same input should give same output") {
    val n = new NoiseInterpolator3D(4, 4, 4, (i, j, k) => i + 2 * j - 3 * k)
    for (i <- 0 until 16; j <- 0 until 16; k <- 0 until 16) {
      assert(n(i, j, k) == n(i, j, k))
    }
  }

  test("input should give correct output") {
    val func: (Int, Int, Int) => Double = (i, j, k) => i + 2 * j - 3 * k
    val n = new NoiseInterpolator3D(4, 4, 4, func)
    for (i <- 0 until 4; j <- 0 until 4; k <- 0 until 4) {
      assertResult(func(i, j, k))(n(4 * i, 4 * j, 4 * k))
    }
  }
}
