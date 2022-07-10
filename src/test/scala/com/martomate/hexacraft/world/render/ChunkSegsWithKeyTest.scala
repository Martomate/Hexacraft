package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.world.render.segment.{ChunkSegsWithKey, Segment}

class ChunkSegsWithKeyTest extends ChunkSegsTest {
  "lastKeyAndSegment" should "return the correct key if nothing is removed" in {
    val segs = make
    segs.add(12, Segment(5, 6))
    segs.add(23, Segment(2, 2))
    segs.lastKeyAndSegment() shouldBe 12 -> Segment(5, 6)
  }
  it should "return the correct key when something is removed in the beginning" in {
    val segs = make
    segs.add(23, Segment(2, 2))
    segs.add(12, Segment(5, 6))
    segs.remove(23, Segment(2, 2))
    segs.lastKeyAndSegment() shouldBe 12 -> Segment(5, 6)
  }
  it should "return the correct key when something is removed in the end" in {
    val segs = make
    segs.add(23, Segment(2, 2))
    segs.add(12, Segment(5, 6))
    segs.remove(12, Segment(5, 6))
    segs.lastKeyAndSegment() shouldBe 23 -> Segment(2, 2)
  }
  it should "return the correct key when something is changed in the end" in {
    val segs = make
    segs.add(23, Segment(2, 2))
    segs.add(12, Segment(5, 6))
    segs.remove(12, Segment(5, 6))
    segs.add(23, Segment(5, 6))
    segs.lastKeyAndSegment() shouldBe 23 -> Segment(5, 6)
  }
  it should "return the correct key after several removals in the end" in {
    val segs = make
    segs.add(23, Segment(2, 2))
    segs.add(12, Segment(45, 6))
    segs.add(12, Segment(35, 6))
    segs.add(12, Segment(25, 6))
    segs.add(12, Segment(5, 6))
    segs.remove(12, Segment(35, 6))
    segs.remove(12, Segment(25, 6))
    segs.remove(12, Segment(45, 6))
    segs.lastKeyAndSegment() shouldBe 12 -> Segment(5, 6)
  }
  it should "return the correct key when part of the last segment is removed" in {
    val segs = make
    segs.add(23, Segment(2, 2))
    segs.add(12, Segment(5, 6))
    segs.remove(12, Segment(5, 2))
    segs.lastKeyAndSegment() shouldBe 12 -> Segment(7, 4)
    segs.remove(12, Segment(8, 3))
    segs.lastKeyAndSegment() shouldBe 12 -> Segment(7, 1)
  }

  "add(T, Segment)" should "complain if the segment already exists, regardless of key" in {
    val segs = make
    segs.add(3, Segment(4, 6))
    assertThrows[IllegalArgumentException](segs.add(4, Segment(4, 6)))
    assertThrows[IllegalArgumentException](segs.add(3, Segment(4, 6)))
    assertThrows[IllegalArgumentException](segs.add(3, Segment(3, 3)))
  }

  "remove(T, Segment)" should "complain if the segment is not part of an existing segment" in {
    val segs = make
    segs.add(3, Segment(4, 6))
    assertThrows[IllegalArgumentException](segs.remove(3, Segment(3, 6)))
    assertThrows[IllegalArgumentException](segs.remove(3, Segment(5, 6)))
    assertThrows[IllegalArgumentException](segs.remove(3, Segment(3, 8)))
    assertThrows[IllegalArgumentException](segs.remove(3, Segment(23, 3)))
  }
  it should "complain if the segment exists, but with another key" in {
    val segs = make
    segs.add(3, Segment(4, 6))
    assertThrows[IllegalArgumentException](segs.remove(4, Segment(4, 6)))
  }
  it should "not remove anything if it fails" in {
    val segs = make
    segs.add(3, Segment(4, 6))
    assertThrows[IllegalArgumentException](segs.remove(4, Segment(6, 2)))
    segs.toSeq shouldBe Seq(Segment(4, 6))
  }

  override def make: ChunkSegsWithKey[Int] = new ChunkSegsWithKey
}
