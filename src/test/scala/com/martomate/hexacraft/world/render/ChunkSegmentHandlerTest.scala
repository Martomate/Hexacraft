package com.martomate.hexacraft.world.render

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class ChunkSegmentHandlerTest extends FlatSpec with Matchers with MockFactory {
  "hasMapping" should "be false if the chunk has never had a mapping" in {
    val handler = make
    val r2 = mkRenderer
    handler.hasMapping(r2) shouldBe false
    handler.add(mkRenderer, Segment(2, 3))
    handler.hasMapping(r2) shouldBe false
  }
  it should "be true if the chunk has a segment in this handler" in {
    val handler = make
    val r2 = mkRenderer
    handler.add(r2, Segment(2, 3))
    handler.hasMapping(r2) shouldBe true
  }
  it should "be false if all the chunk's segments have been removed" in {
    val handler = make
    val r2 = mkRenderer
    handler.add(r2, Segment(2, 3))
    handler.remove(r2, Segment(2, 3))
    handler.hasMapping(r2) shouldBe false
  }

  "length" should "be 0 in the beginning" in {
    make.length shouldBe 0
  }
  it should "be equal to the maximum size Segment(0, size) to contain all the segments" in {
    val handler = make
    handler.add(mkRenderer, Segment(5, 6))
    handler.length shouldBe 5 + 6
  }
  it should "remain correct even after removals at the end" in {
    val handler = make
    val r1 = mkRenderer
    handler.add(r1, Segment(0, 2))
    handler.add(r1, Segment(2, 3))
    handler.add(r1, Segment(5, 6))
    handler.remove(r1, Segment(2, 3))
    handler.remove(r1, Segment(5, 6))
    handler.length shouldBe 2
  }

  "totalLengthForChunk" should "be 0 in the beginning" in {
    make.totalLengthForChunk(mkRenderer) shouldBe 0
  }
  it should "be 0 for chunks without mapping" in {
    val handler = make
    handler.add(mkRenderer, Segment(5, 3))
    handler.totalLengthForChunk(mkRenderer) shouldBe 0
  }
  it should "not count the holes of a chunk's segments" in {
    val handler = make
    val r1 = mkRenderer
    handler.add(r1, Segment(5, 3))
    handler.add(r1, Segment(15, 3))
    handler.totalLengthForChunk(r1) shouldBe 6
  }

  "add" should "add the segment if there is nothing there" in {
    val handler = make
    val r1 = mkRenderer
    handler.add(r1, Segment(4, 3))
    handler.segments(r1).toSeq shouldBe Seq(Segment(4, 3))
  }
  it should "complain if there is overlap for the same chunk" in {
    val handler = make
    val r1 = mkRenderer
    handler.add(r1, Segment(4, 3))
    assertThrows[IllegalArgumentException](handler.add(r1, Segment(4, 3)))
    assertThrows[IllegalArgumentException](handler.add(r1, Segment(3, 3)))
    assertThrows[IllegalArgumentException](handler.add(r1, Segment(5, 3)))
    assertThrows[IllegalArgumentException](handler.add(r1, Segment(3, 10)))
    assertThrows[IllegalArgumentException](handler.add(r1, Segment(5, 1)))
    handler.segments(r1).toSeq shouldBe Seq(Segment(4, 3))
  }
  it should "complain if there is overlap for another chunk" in {
    val handler = make
    val r1 = mkRenderer
    val r2 = mkRenderer
    handler.add(r1, Segment(4, 3))
    assertThrows[IllegalArgumentException](handler.add(r2, Segment(4, 3)))
    assertThrows[IllegalArgumentException](handler.add(r2, Segment(3, 3)))
    assertThrows[IllegalArgumentException](handler.add(r2, Segment(5, 3)))
    assertThrows[IllegalArgumentException](handler.add(r2, Segment(3, 10)))
    assertThrows[IllegalArgumentException](handler.add(r2, Segment(5, 1)))
    handler.segments(r1).toSeq shouldBe Seq(Segment(4, 3))
  }

  "remove" should "remove the segment if it exists among the segments of this chunk" in {
    val handler = make
    val r1 = mkRenderer
    handler.add(r1, Segment(5, 3))
    handler.remove(r1, Segment(5, 3))
    handler.segments(r1).toSeq shouldBe empty
  }
  it should "remove the segment if it is contained in an existing segment on this chunk" in {
    val handler = make
    val r1 = mkRenderer
    handler.add(r1, Segment(5, 3))
    handler.remove(r1, Segment(6, 1))
    handler.segments(r1).toSeq shouldBe Seq(Segment(5, 1), Segment(7, 1))
  }
  it should "complain if the chunk doesn't have a matching segment" in {
    val handler = make
    val r1 = mkRenderer
    handler.add(r1, Segment(5, 3))
    assertThrows[IllegalArgumentException](handler.remove(r1, Segment(16, 5)))
    assertThrows[IllegalArgumentException](handler.remove(r1, Segment(5, 5)))
    handler.segments(r1).toSeq shouldBe Seq(Segment(5, 3))
  }
  it should "complain if there is a matching segment but on the wrong chunk" in {
    val handler = make
    val r1 = mkRenderer
    handler.add(r1, Segment(5, 3))
    assertThrows[IllegalArgumentException](handler.remove(mkRenderer, Segment(5, 3)))
    assertThrows[IllegalArgumentException](handler.remove(mkRenderer, Segment(6, 1)))
    handler.segments(r1).toSeq shouldBe Seq(Segment(5, 3))
  }

  "segments" should "be empty if the chunk doesn't have a mapping" in {
    val handler = make
    handler.add(mkRenderer, Segment(2, 1))
    handler.segments(mkRenderer) shouldBe empty
  }
  it should "be correct if the chunk has a mapping" in {
    val handler = make
    val r1 = mkRenderer
    handler.add(r1, Segment(2, 1))
    handler.add(r1, Segment(32, 4))
    handler.add(r1, Segment(12, 11))
    handler.segments(r1).toSeq shouldBe Seq(Segment(2, 1), Segment(12, 11), Segment(32, 4))
  }

  "lastSegment" should "be None if there are no segments" in {
    make.lastSegment() shouldBe None
  }
  it should "work if nothing is removed" in {
    val handler = make
    val r1 = mkRenderer
    handler.add(r1, Segment(2, 1))
    handler.lastSegment() shouldBe Some((r1, Segment(2, 1)))
    handler.lastSegment() shouldBe Some((r1, Segment(2, 1)))
    handler.add(r1, Segment(32, 4))
    handler.add(r1, Segment(12, 11))
    handler.lastSegment() shouldBe Some((r1, Segment(32, 4)))
  }
  it should "work if something is removed" in {
    val handler = make
    val r1 = mkRenderer
    handler.add(r1, Segment(2, 1))
    handler.add(r1, Segment(12, 11))
    handler.add(r1, Segment(32, 4))
    handler.remove(r1, Segment(12, 11))
    handler.lastSegment() shouldBe Some((r1, Segment(32, 4)))
    handler.remove(r1, Segment(32, 4))
    handler.lastSegment() shouldBe Some((r1, Segment(2, 1)))
  }
  it should "work if something is removed and added with another chunk" in {
    val handler = make
    val r1 = mkRenderer
    val r2 = mkRenderer
    handler.add(r1, Segment(2, 1))
    handler.add(r1, Segment(12, 11))
    handler.add(r1, Segment(32, 4))
    handler.remove(r1, Segment(12, 11))
    handler.add(r2, Segment(12, 11))
    handler.remove(r1, Segment(32, 4))
    handler.lastSegment() shouldBe Some((r2, Segment(12, 11)))
  }

  def make: ChunkSegmentHandler = new ChunkSegmentHandler
  def mkRenderer: ChunkRenderer = stub[ChunkRenderer]
}
