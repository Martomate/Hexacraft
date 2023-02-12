package com.martomate.hexacraft.world.render.segment

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import com.martomate.hexacraft.world.render.segment.{ChunkSegmentHandler, Segment}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ChunkSegmentHandlerTest extends AnyFlatSpec with Matchers {
  given CylinderSize = CylinderSize(4)

  "hasMapping" should "be false if the chunk has never had a mapping" in {
    val handler = new ChunkSegmentHandler
    val coords1 = ChunkRelWorld(1, 2, 3)
    val coords2 = ChunkRelWorld(1, 2, 4)
    handler.hasMapping(coords1) shouldBe false
    handler.add(coords2, Segment(2, 3))
    handler.hasMapping(coords1) shouldBe false
  }
  it should "be true if the chunk has a segment in this handler" in {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(2, 3))
    handler.hasMapping(coords) shouldBe true
  }
  it should "be false if all the chunk's segments have been removed" in {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(2, 3))
    handler.remove(coords, Segment(2, 3))
    handler.hasMapping(coords) shouldBe false
  }

  "length" should "be 0 in the beginning" in {
    (new ChunkSegmentHandler).length shouldBe 0
  }
  it should "be equal to the maximum size Segment(0, size) to contain all the segments" in {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(5, 6))
    handler.length shouldBe 5 + 6
  }
  it should "remain correct even after removals at the end" in {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(0, 2))
    handler.add(coords, Segment(2, 3))
    handler.add(coords, Segment(5, 6))
    handler.remove(coords, Segment(2, 3))
    handler.remove(coords, Segment(5, 6))
    handler.length shouldBe 2
  }

  "totalLengthForChunk" should "be 0 in the beginning" in {
    val coords = ChunkRelWorld(1, 2, 3)
    (new ChunkSegmentHandler).totalLengthForChunk(coords) shouldBe 0
  }
  it should "be 0 for chunks without mapping" in {
    val handler = new ChunkSegmentHandler
    val coords1 = ChunkRelWorld(1, 2, 3)
    val coords2 = ChunkRelWorld(1, 2, 4)
    handler.add(coords1, Segment(5, 3))
    handler.totalLengthForChunk(coords2) shouldBe 0
  }
  it should "not count the holes of a chunk's segments" in {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(5, 3))
    handler.add(coords, Segment(15, 3))
    handler.totalLengthForChunk(coords) shouldBe 6
  }

  "add" should "add the segment if there is nothing there" in {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(4, 3))
    handler.segments(coords).toSeq shouldBe Seq(Segment(4, 3))
  }
  it should "complain if there is overlap for the same chunk" in {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(4, 3))
    assertThrows[IllegalArgumentException](handler.add(coords, Segment(4, 3)))
    assertThrows[IllegalArgumentException](handler.add(coords, Segment(3, 3)))
    assertThrows[IllegalArgumentException](handler.add(coords, Segment(5, 3)))
    assertThrows[IllegalArgumentException](handler.add(coords, Segment(3, 10)))
    assertThrows[IllegalArgumentException](handler.add(coords, Segment(5, 1)))
    handler.segments(coords).toSeq shouldBe Seq(Segment(4, 3))
  }
  it should "complain if there is overlap for another chunk" in {
    val handler = new ChunkSegmentHandler
    val coords1 = ChunkRelWorld(1, 2, 3)
    val coords2 = ChunkRelWorld(1, 2, 4)
    handler.add(coords1, Segment(4, 3))
    assertThrows[IllegalArgumentException](handler.add(coords2, Segment(4, 3)))
    assertThrows[IllegalArgumentException](handler.add(coords2, Segment(3, 3)))
    assertThrows[IllegalArgumentException](handler.add(coords2, Segment(5, 3)))
    assertThrows[IllegalArgumentException](handler.add(coords2, Segment(3, 10)))
    assertThrows[IllegalArgumentException](handler.add(coords2, Segment(5, 1)))
    handler.segments(coords1).toSeq shouldBe Seq(Segment(4, 3))
  }

  "remove" should "remove the segment if it exists among the segments of this chunk" in {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(5, 3))
    handler.remove(coords, Segment(5, 3))
    handler.segments(coords).toSeq shouldBe empty
  }
  it should "remove the segment if it is contained in an existing segment on this chunk" in {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(5, 3))
    handler.remove(coords, Segment(6, 1))
    handler.segments(coords).toSeq shouldBe Seq(Segment(5, 1), Segment(7, 1))
  }
  it should "complain if the chunk doesn't have a matching segment" in {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(5, 3))
    assertThrows[IllegalArgumentException](handler.remove(coords, Segment(16, 5)))
    assertThrows[IllegalArgumentException](handler.remove(coords, Segment(5, 5)))
    handler.segments(coords).toSeq shouldBe Seq(Segment(5, 3))
  }
  it should "complain if there is a matching segment but on the wrong chunk" in {
    val handler = new ChunkSegmentHandler
    val coords1 = ChunkRelWorld(1, 2, 3)
    val coords2 = ChunkRelWorld(1, 2, 4)
    val coords3 = ChunkRelWorld(1, 2, 5)
    handler.add(coords1, Segment(5, 3))
    assertThrows[IllegalArgumentException](handler.remove(coords2, Segment(5, 3)))
    assertThrows[IllegalArgumentException](handler.remove(coords3, Segment(6, 1)))
    handler.segments(coords1).toSeq shouldBe Seq(Segment(5, 3))
  }

  "segments" should "be empty if the chunk doesn't have a mapping" in {
    val handler = new ChunkSegmentHandler
    val coords1 = ChunkRelWorld(1, 2, 3)
    val coords2 = ChunkRelWorld(1, 2, 4)
    handler.add(coords1, Segment(2, 1))
    handler.segments(coords2) shouldBe empty
  }
  it should "be correct if the chunk has a mapping" in {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(2, 1))
    handler.add(coords, Segment(32, 4))
    handler.add(coords, Segment(12, 11))
    handler.segments(coords).toSeq shouldBe Seq(Segment(2, 1), Segment(12, 11), Segment(32, 4))
  }

  "lastSegment" should "be None if there are no segments" in {
    (new ChunkSegmentHandler).lastSegment() shouldBe None
  }
  it should "work if nothing is removed" in {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(2, 1))
    handler.lastSegment() shouldBe Some((coords, Segment(2, 1)))
    handler.lastSegment() shouldBe Some((coords, Segment(2, 1)))
    handler.add(coords, Segment(32, 4))
    handler.add(coords, Segment(12, 11))
    handler.lastSegment() shouldBe Some((coords, Segment(32, 4)))
  }
  it should "work if something is removed" in {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(2, 1))
    handler.add(coords, Segment(12, 11))
    handler.add(coords, Segment(32, 4))
    handler.remove(coords, Segment(12, 11))
    handler.lastSegment() shouldBe Some((coords, Segment(32, 4)))
    handler.remove(coords, Segment(32, 4))
    handler.lastSegment() shouldBe Some((coords, Segment(2, 1)))
  }
  it should "work if something is removed and added with another chunk" in {
    val handler = new ChunkSegmentHandler
    val coords1 = ChunkRelWorld(1, 2, 3)
    val coords2 = ChunkRelWorld(1, 2, 4)
    handler.add(coords1, Segment(2, 1))
    handler.add(coords1, Segment(12, 11))
    handler.add(coords1, Segment(32, 4))
    handler.remove(coords1, Segment(12, 11))
    handler.add(coords2, Segment(12, 11))
    handler.remove(coords1, Segment(32, 4))
    handler.lastSegment() shouldBe Some((coords2, Segment(12, 11)))
  }
}
