package com.martomate.hexacraft.world.render.segment

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld
import com.martomate.hexacraft.world.render.segment.{ChunkSegmentHandler, Segment}

import munit.FunSuite

class ChunkSegmentHandlerTest extends FunSuite {
  given CylinderSize = CylinderSize(4)

  test("hasMapping should be false if the chunk has never had a mapping") {
    val handler = new ChunkSegmentHandler
    val coords1 = ChunkRelWorld(1, 2, 3)
    val coords2 = ChunkRelWorld(1, 2, 4)
    assert(!handler.hasMapping(coords1))
    handler.add(coords2, Segment(2, 3))
    assert(!handler.hasMapping(coords1))
  }
  test("hasMapping should be true if the chunk has a segment in this handler") {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(2, 3))
    assert(handler.hasMapping(coords))
  }
  test("hasMapping should be false if all the chunk's segments have been removed") {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(2, 3))
    handler.remove(coords, Segment(2, 3))
    assert(!handler.hasMapping(coords))
  }

  test("length should be 0 in the beginning") {
    assertEquals((new ChunkSegmentHandler).length, 0)
  }
  test("length should be equal to the maximum size Segment(0, size) to contain all the segments") {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(5, 6))
    assertEquals(handler.length, 5 + 6)
  }
  test("length should remain correct even after removals at the end") {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(0, 2))
    handler.add(coords, Segment(2, 3))
    handler.add(coords, Segment(5, 6))
    handler.remove(coords, Segment(2, 3))
    handler.remove(coords, Segment(5, 6))
    assertEquals(handler.length, 2)
  }

  test("totalLengthForChunk should be 0 in the beginning") {
    val coords = ChunkRelWorld(1, 2, 3)
    assertEquals((new ChunkSegmentHandler).totalLengthForChunk(coords), 0)
  }
  test("totalLengthForChunk should be 0 for chunks without mapping") {
    val handler = new ChunkSegmentHandler
    val coords1 = ChunkRelWorld(1, 2, 3)
    val coords2 = ChunkRelWorld(1, 2, 4)
    handler.add(coords1, Segment(5, 3))
    assertEquals(handler.totalLengthForChunk(coords2), 0)
  }
  test("totalLengthForChunk should not count the holes of a chunk's segments") {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(5, 3))
    handler.add(coords, Segment(15, 3))
    assertEquals(handler.totalLengthForChunk(coords), 6)
  }

  test("add should add the segment if there is nothing there") {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(4, 3))
    assertEquals(handler.segments(coords).toSeq, Seq(Segment(4, 3)))
  }
  test("add should complain if there is overlap for the same chunk") {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(4, 3))
    intercept[IllegalArgumentException](handler.add(coords, Segment(4, 3)))
    intercept[IllegalArgumentException](handler.add(coords, Segment(3, 3)))
    intercept[IllegalArgumentException](handler.add(coords, Segment(5, 3)))
    intercept[IllegalArgumentException](handler.add(coords, Segment(3, 10)))
    intercept[IllegalArgumentException](handler.add(coords, Segment(5, 1)))
    assertEquals(handler.segments(coords).toSeq, Seq(Segment(4, 3)))
  }
  test("add should complain if there is overlap for another chunk") {
    val handler = new ChunkSegmentHandler
    val coords1 = ChunkRelWorld(1, 2, 3)
    val coords2 = ChunkRelWorld(1, 2, 4)
    handler.add(coords1, Segment(4, 3))
    intercept[IllegalArgumentException](handler.add(coords2, Segment(4, 3)))
    intercept[IllegalArgumentException](handler.add(coords2, Segment(3, 3)))
    intercept[IllegalArgumentException](handler.add(coords2, Segment(5, 3)))
    intercept[IllegalArgumentException](handler.add(coords2, Segment(3, 10)))
    intercept[IllegalArgumentException](handler.add(coords2, Segment(5, 1)))
    assertEquals(handler.segments(coords1).toSeq, Seq(Segment(4, 3)))
  }

  test("remove should remove the segment if it exists among the segments of this chunk") {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(5, 3))
    handler.remove(coords, Segment(5, 3))
    assert(handler.segments(coords).toSeq.isEmpty)
  }
  test("remove should remove the segment if it is contained in an existing segment on this chunk") {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(5, 3))
    handler.remove(coords, Segment(6, 1))
    assertEquals(handler.segments(coords).toSeq, Seq(Segment(5, 1), Segment(7, 1)))
  }
  test("remove should complain if the chunk doesn't have a matching segment") {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(5, 3))
    intercept[IllegalArgumentException](handler.remove(coords, Segment(16, 5)))
    intercept[IllegalArgumentException](handler.remove(coords, Segment(5, 5)))
    assertEquals(handler.segments(coords).toSeq, Seq(Segment(5, 3)))
  }
  test("remove should complain if there is a matching segment but on the wrong chunk") {
    val handler = new ChunkSegmentHandler
    val coords1 = ChunkRelWorld(1, 2, 3)
    val coords2 = ChunkRelWorld(1, 2, 4)
    val coords3 = ChunkRelWorld(1, 2, 5)
    handler.add(coords1, Segment(5, 3))
    intercept[IllegalArgumentException](handler.remove(coords2, Segment(5, 3)))
    intercept[IllegalArgumentException](handler.remove(coords3, Segment(6, 1)))
    assertEquals(handler.segments(coords1).toSeq, Seq(Segment(5, 3)))
  }

  test("segments should be empty if the chunk doesn't have a mapping") {
    val handler = new ChunkSegmentHandler
    val coords1 = ChunkRelWorld(1, 2, 3)
    val coords2 = ChunkRelWorld(1, 2, 4)
    handler.add(coords1, Segment(2, 1))
    assert(handler.segments(coords2).isEmpty)
  }
  test("segments should be correct if the chunk has a mapping") {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(2, 1))
    handler.add(coords, Segment(32, 4))
    handler.add(coords, Segment(12, 11))
    assertEquals(handler.segments(coords).toSeq, Seq(Segment(2, 1), Segment(12, 11), Segment(32, 4)))
  }

  test("lastSegment should be None if there are no segments") {
    assertEquals((new ChunkSegmentHandler).lastSegment(), None)
  }
  test("lastSegment should work if nothing is removed") {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(2, 1))
    assertEquals(handler.lastSegment(), Some((coords, Segment(2, 1))))
    assertEquals(handler.lastSegment(), Some((coords, Segment(2, 1))))
    handler.add(coords, Segment(32, 4))
    handler.add(coords, Segment(12, 11))
    assertEquals(handler.lastSegment(), Some((coords, Segment(32, 4))))
  }
  test("lastSegment should work if something is removed") {
    val handler = new ChunkSegmentHandler
    val coords = ChunkRelWorld(1, 2, 3)
    handler.add(coords, Segment(2, 1))
    handler.add(coords, Segment(12, 11))
    handler.add(coords, Segment(32, 4))
    handler.remove(coords, Segment(12, 11))
    assertEquals(handler.lastSegment(), Some((coords, Segment(32, 4))))
    handler.remove(coords, Segment(32, 4))
    assertEquals(handler.lastSegment(), Some((coords, Segment(2, 1))))
  }
  test("lastSegment should work if something is removed and added with another chunk") {
    val handler = new ChunkSegmentHandler
    val coords1 = ChunkRelWorld(1, 2, 3)
    val coords2 = ChunkRelWorld(1, 2, 4)
    handler.add(coords1, Segment(2, 1))
    handler.add(coords1, Segment(12, 11))
    handler.add(coords1, Segment(32, 4))
    handler.remove(coords1, Segment(12, 11))
    handler.add(coords2, Segment(12, 11))
    handler.remove(coords1, Segment(32, 4))
    assertEquals(handler.lastSegment(), Some((coords2, Segment(12, 11))))
  }
}
