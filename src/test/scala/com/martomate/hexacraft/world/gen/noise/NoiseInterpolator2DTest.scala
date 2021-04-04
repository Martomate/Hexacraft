package com.martomate.hexacraft.world.gen.noise

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NoiseInterpolator2DTest extends AnyFlatSpec with Matchers {
  "noise" should "be the same for the same input" in {
    val n = new NoiseInterpolator2D(4, 4, (i, j) => i + 2 * j)
    for (i <- 0 until 16; j <- 0 until 16) {
      n(i, j) shouldBe n(i, j)
    }
  }

  it should "be correct" in {
    val func: (Int, Int) => Double = (i, j) => i + 2 * j
    val n = new NoiseInterpolator2D(4, 4, func)
    for (i <- 0 until 4; j <- 0 until 4) {
      n(4 * i, 4 * j) shouldBe func(i, j)
    }
  }
}
