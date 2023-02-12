package com.martomate.hexacraft.util

import munit.FunSuite

class ConstantSeqTest extends FunSuite {
  val seq = new ConstantSeq[String](3, "a string")

  test("length should be the size of the seq") {
    assertEquals(seq.length, 3)
  }

  test("length must be non-negative") {
    // this should fail
    intercept[IllegalArgumentException](new ConstantSeq[String](-1, "a string"))

    // this should be fine
    new ConstantSeq[String](0, "a string")
  }

  test("apply should work for indices within [0, length)") {
    assertEquals(seq(0), "a string")
    assertEquals(seq(1), "a string")
    assertEquals(seq(2), "a string")
  }

  test("for loops should run length steps") {
    var count = 0
    for (a <- seq) {
      count += 1
      assertEquals(a, "a string")
    }
    assertEquals(count, 3)
  }

  test("The seq should use constant space") {
    // no exception should be thrown
    new ConstantSeq[String](Int.MaxValue, "A string of great length!!")
  }
}
