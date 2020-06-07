package com.martomate.hexacraft.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SeqUtilsTest extends AnyFlatSpec with Matchers {
  "whileSome" should "do a maximum of maxCount iterations" in {
    var total = 0
    SeqUtils.whileSome(10, Some(4)) { t =>
      total += t
    }
    total shouldBe 40
  }
  it should "stop if maker == None" in {
    var total = 0
    SeqUtils.whileSome(10, if (total < 32) Some(4) else None) { t =>
      total += t
    }
    total shouldBe 32
  }
}
