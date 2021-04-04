package com.martomate.hexacraft.world.gen.noise

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NoiseInterpolator3DTest extends AnyFlatSpec with Matchers {
  "noise" should "be the same for the same input" in {
    val n = new NoiseInterpolator3D(4, 4, 4, (i, j, k) => i + 2 * j - 3 * k)

    for (i <- 0 until 16; j <- 0 until 16; k <- 0 until 16) {
      n(i, j, k) shouldBe n(i, j, k)
    }
  }

  it should "give the correct output" in {
    val func: (Int, Int, Int) => Double = (i, j, k) => i + 2 * j - 3 * k
    val n = new NoiseInterpolator3D(4, 4, 4, func)

    for (i <- 0 until 4; j <- 0 until 4; k <- 0 until 4) {
      n(4 * i, 4 * j, 4 * k) shouldBe func(i, j, k)
    }
  }
}
