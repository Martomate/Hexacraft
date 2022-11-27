package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.world.render.segment.Segment

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SegmentTest extends AnyFlatSpec with Matchers {
  "Segment" should "fail for negative 'start'" in {
    Segment(4, 5)
    Segment(0, 5)
    assertThrows[IllegalArgumentException](Segment(-2, 3))
    assertThrows[IllegalArgumentException](Segment(-2, 1))
  }
  it should "fail for non-positive 'length'" in {
    assertThrows[IllegalArgumentException](Segment(3, -2))
    assertThrows[IllegalArgumentException](Segment(0, -10))
    assertThrows[IllegalArgumentException](Segment(5, 0))
  }

  "contains" should "return false for segments completely to the left" in {
    Segment(5, 3).contains(Segment(2, 3)) shouldBe false
  }
  it should "return false for segments completely to the right" in {
    Segment(5, 3).contains(Segment(8, 3)) shouldBe false
  }
  it should "return false for segments partially to the left" in {
    Segment(5, 3).contains(Segment(4, 3)) shouldBe false
  }
  it should "return false for segments partially to the right" in {
    Segment(5, 3).contains(Segment(7, 3)) shouldBe false
  }
  it should "return false for segments containing this one" in {
    Segment(5, 3).contains(Segment(4, 7)) shouldBe false
  }
  it should "return true for segments contained in this one" in {
    Segment(5, 3).contains(Segment(5, 3)) shouldBe true
    Segment(5, 3).contains(Segment(5, 2)) shouldBe true
    Segment(5, 3).contains(Segment(6, 2)) shouldBe true
  }

  "overlaps" should "return false for segments completely to the left" in {
    Segment(5, 3).overlaps(Segment(2, 3)) shouldBe false
  }
  it should "return false for segments completely to the right" in {
    Segment(5, 3).overlaps(Segment(8, 3)) shouldBe false
  }
  it should "return true for segments partially to the left" in {
    Segment(5, 3).overlaps(Segment(4, 3)) shouldBe true
  }
  it should "return true for segments partially to the right" in {
    Segment(5, 3).overlaps(Segment(7, 3)) shouldBe true
  }
  it should "return true for segments containing this one" in {
    Segment(5, 3).overlaps(Segment(4, 7)) shouldBe true
  }
  it should "return true for segments contained in this one" in {
    Segment(5, 3).overlaps(Segment(5, 3)) shouldBe true
    Segment(5, 3).overlaps(Segment(5, 2)) shouldBe true
    Segment(5, 3).overlaps(Segment(6, 2)) shouldBe true
  }
}
