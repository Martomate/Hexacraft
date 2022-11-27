package com.martomate.hexacraft.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MathUtilsTest extends AnyFlatSpec with Matchers {
  "fitZ" should "work correctly" in {
    MathUtils.fitZ(123.54, 200) shouldBe 123.54
    MathUtils.fitZ(123.54 - 200, 200) shouldBe 123.54
  }
}
