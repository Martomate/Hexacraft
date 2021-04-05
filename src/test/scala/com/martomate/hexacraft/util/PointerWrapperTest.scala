package com.martomate.hexacraft.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PointerWrapperTest extends AnyFlatSpec with Matchers {
  "ints" should "apply the function" in {
    val pw = new PointerWrapper
    var applied = false
    pw.ints((_, _) => applied = true)
    applied shouldBe true
  }

  it should "pass single-element arrays as parameters" in {
    val pw = new PointerWrapper
    pw.ints { (px, py) =>
      px shouldBe a [Array[_]]
      py shouldBe a [Array[_]]

      px.length shouldBe 1
      py.length shouldBe 1
    }
  }

  it should "return the first values of the arrays" in {
    val pw = new PointerWrapper
    val (rx, ry) = pw.ints { (px, py) =>
      px(0) = 7
      py(0) = 4
    }
    rx shouldBe 7
    ry shouldBe 4
  }

  "doubles" should "apply the function" in {
    val pw = new PointerWrapper
    var applied = false
    pw.doubles((_, _) => applied = true)
    applied shouldBe true
  }

  it should "pass single-element arrays as parameters" in {
    val pw = new PointerWrapper
    pw.doubles { (px, py) =>
      px shouldBe a [Array[_]]
      py shouldBe a [Array[_]]

      px.length shouldBe 1
      py.length shouldBe 1
    }
  }

  it should "return the first values of the arrays" in {
    val pw = new PointerWrapper
    val (rx, ry) = pw.doubles { (px, py) =>
      px(0) = 7.3
      py(0) = 4.2
    }
    rx shouldBe 7.3
    ry shouldBe 4.2
  }
}
