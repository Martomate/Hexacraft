package com.martomate.hexacraft.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SmartArrayTest extends AnyFlatSpec with Matchers {
  "SmartArray.apply" should "create a SmartArray with correct default element" in {
    val default = "abc"
    val arr = SmartArray(5, default)(s => new Array(s))
    arr(0) shouldBe default
    arr(4) shouldBe default
  }
  it should "use the default value if nothing else exists" in {
    val default = "abc"
    val arr = SmartArray(5, default)(s => new Array(s))
    arr(1) = default
    arr(2) = "a great string"
    arr(0) shouldBe default
    arr(1) shouldBe default
    arr(2) shouldBe "a great string"
    arr(4) shouldBe default
  }
  "SmartArray.withByteArray" should "create a SmartArray with correct default element" in {
    val default = 3.toByte
    val arr = SmartArray.withByteArray(5, default)
    arr(0) shouldBe default
    arr(4) shouldBe default
  }
  it should "use the default value if nothing else exists" in {
    val default = 3.toByte
    val arr = SmartArray.withByteArray(5, default)
    arr(1) = default
    arr(2) = 8.toByte
    arr(0) shouldBe default
    arr(1) shouldBe default
    arr(2) shouldBe 8.toByte
    arr(4) shouldBe default
  }
  "length" should "return the length of the array" in {
    val arr = SmartArray(7, "a")(s => new Array(s))
    arr.length shouldBe 7
    arr(2) = "bcd"
    arr.length shouldBe 7
    arr(2) = "a"
    arr.length shouldBe 7
  }
}
