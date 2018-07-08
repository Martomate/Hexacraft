package com.martomate.hexacraft.util

import org.scalatest.{FlatSpec, Matchers}

class ConstantSeqTest extends FlatSpec with Matchers {
  val seq = new ConstantSeq[String](3, "a string")

  "length" should "be the size of the seq" in {
    seq.length shouldBe 3
  }

  it must "be non-negative" in {
    an [IllegalArgumentException] should be thrownBy new ConstantSeq[String](-1, "a string")
    noException should be thrownBy new ConstantSeq[String](0, "a string")
  }

  "apply" should "work for indices within [0, length)" in {
    seq(0) shouldBe "a string"
    seq(1) shouldBe "a string"
    seq(2) shouldBe "a string"
  }

  "for loops" should "run length steps" in {
    var count = 0
    for (a <- seq) {
      count += 1
      a shouldBe "a string"
    }
    count shouldBe 3
  }

  "The seq" should "use constant space" in {
    noException should be thrownBy new ConstantSeq[String](Int.MaxValue, "A string of great length!!")
  }
}
