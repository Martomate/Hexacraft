package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.world.render.segment.{ChunkSegs, Segment}

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ChunkSegsTest extends AnyFlatSpec with Matchers {
  "add" should "add the segment if it doesn't exist" in {
    val segs = make
    val seg = Segment(5, 8)
    segs.add(seg)
    segs.toSeq shouldBe Seq(seg)
  }
  it should "complain if it already exists" in {
    val segs = make
    val seg = Segment(5, 8)
    segs.add(seg)
    assertThrows[IllegalArgumentException](segs.add(seg))
  }
  it should "complain if it overlaps an existing segment" in {
    val segs = make
    segs.add(Segment(5, 8))
    assertThrows[IllegalArgumentException](segs.add(Segment(6, 4)))
    assertThrows[IllegalArgumentException](segs.add(Segment(6, 12)))
    assertThrows[IllegalArgumentException](segs.add(Segment(2, 4)))
    assertThrows[IllegalArgumentException](segs.add(Segment(2, 12)))
  }
  it should "not complain if it doesn't overlap an existing segment" in {
    val segs = make
    segs.add(Segment(5, 8))
    segs.add(Segment(2, 3))
    segs.add(Segment(13, 3))
  }

  "remove" should "remove the segment if it exists, and return true" in {
    val segs = make
    segs.add(Segment(6, 23))
    segs.remove(Segment(6, 23)) shouldBe true
    segs.toSeq shouldBe empty
  }
  it should "not remove it if it doesn't exist" in {
    val segs = make
    segs.add(Segment(2, 3))
    segs.remove(Segment(6, 23)) shouldBe false
    segs.toSeq shouldBe Seq(Segment(2, 3))
  }
  it should "remove this part of an existing segment that contains this" in {
    val segs = make
    segs.add(Segment(2, 43))
    segs.remove(Segment(6, 23)) shouldBe true
    segs.toSeq shouldBe Seq(Segment(2, 4), Segment(2 + 4 + 23, 43 - 4 - 23))
  }
  it should "not remove it if there is only partial overlap" in {
    val segs = make
    segs.add(Segment(2, 13))
    segs.remove(Segment(6, 23)) shouldBe false
    segs.toSeq shouldBe Seq(Segment(2, 13))
  }

  "totalLength" should "be 0 for a new ChunkSegs" in {
    make.totalLength shouldBe 0
  }
  it should "be the sum of the lengths of the segments" in {
    val segs = make
    segs.add(Segment(16, 5))
    segs.add(Segment(2, 4))
    segs.add(Segment(12, 1))
    segs.totalLength shouldBe 5 + 4 + 1
  }

  "firstSegment" should "be the segment with lowest 'start'" in {
    val segs = make
    segs.add(Segment(16, 5))
    segs.add(Segment(2, 4))
    segs.add(Segment(12, 1))
    segs.firstSegment() shouldBe Segment(2, 4)
  }
  it should "be correct even if the first element has been removed" in {
    val segs = make
    segs.add(Segment(16, 5))
    segs.add(Segment(2, 4))
    segs.add(Segment(12, 1))
    segs.remove(Segment(2, 2))
    segs.firstSegment() shouldBe Segment(4, 2)
  }

  "lastSegment" should "be the segment with highest 'start'" in {
    val segs = make
    segs.add(Segment(16, 5))
    segs.add(Segment(2, 4))
    segs.add(Segment(12, 1))
    segs.lastSegment() shouldBe Segment(16, 5)
  }
  it should "be correct even if the last element has been removed" in {
    val segs = make
    segs.add(Segment(16, 5))
    segs.add(Segment(2, 4))
    segs.add(Segment(12, 1))
    segs.remove(Segment(18, 3))
    segs.lastSegment() shouldBe Segment(16, 2)
  }

  "iterator" should "be empty if there are no segments" in {
    make.iterator shouldBe empty
  }
  it should "return all the segments in order" in {
    val segs = make
    segs.add(Segment(16, 5))
    segs.add(Segment(2, 4))
    segs.add(Segment(12, 1))
    segs.remove(Segment(18, 3))

    segs.iterator.toSeq shouldBe Seq(Segment(2, 4), Segment(12, 1), Segment(16, 2))
  }

  def make: ChunkSegs = new ChunkSegs
}
