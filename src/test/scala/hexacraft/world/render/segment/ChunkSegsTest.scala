package hexacraft.world.render.segment

import hexacraft.world.render.segment.{ChunkSegs, Segment}

import munit.FunSuite

class ChunkSegsTest extends FunSuite {
  test("add should add the segment if it doesn't exist") {
    val segs = make
    val seg = Segment(5, 8)
    segs.add(seg)
    assertEquals(segs.toSeq, Seq(seg))
  }
  test("add should complain if it already exists") {
    val segs = make
    val seg = Segment(5, 8)
    segs.add(seg)
    intercept[IllegalArgumentException](segs.add(seg))
  }
  test("add should complain if it overlaps an existing segment") {
    val segs = make
    segs.add(Segment(5, 8))
    intercept[IllegalArgumentException](segs.add(Segment(6, 4)))
    intercept[IllegalArgumentException](segs.add(Segment(6, 12)))
    intercept[IllegalArgumentException](segs.add(Segment(2, 4)))
    intercept[IllegalArgumentException](segs.add(Segment(2, 12)))
  }
  test("add should not complain if it doesn't overlap an existing segment") {
    val segs = make
    segs.add(Segment(5, 8))
    segs.add(Segment(2, 3))
    segs.add(Segment(13, 3))
  }

  test("remove should remove the segment if it exists, and return true") {
    val segs = make
    segs.add(Segment(6, 23))
    assert(segs.remove(Segment(6, 23)))
    assert(segs.toSeq.isEmpty)
  }
  test("remove should not remove it if it doesn't exist") {
    val segs = make
    segs.add(Segment(2, 3))
    assert(!segs.remove(Segment(6, 23)))
    assertEquals(segs.toSeq, Seq(Segment(2, 3)))
  }
  test("remove should remove this part of an existing segment that contains this") {
    val segs = make
    segs.add(Segment(2, 43))
    assert(segs.remove(Segment(6, 23)))
    assertEquals(segs.toSeq, Seq(Segment(2, 4), Segment(2 + 4 + 23, 43 - 4 - 23)))
  }
  test("remove should not remove it if there is only partial overlap") {
    val segs = make
    segs.add(Segment(2, 13))
    assert(!segs.remove(Segment(6, 23)))
    assertEquals(segs.toSeq, Seq(Segment(2, 13)))
  }

  test("totalLength should be 0 for a new ChunkSegs") {
    assertEquals(make.totalLength, 0)
  }
  test("totalLength should be the sum of the lengths of the segments") {
    val segs = make
    segs.add(Segment(16, 5))
    segs.add(Segment(2, 4))
    segs.add(Segment(12, 1))
    assertEquals(segs.totalLength, 5 + 4 + 1)
  }

  test("firstSegment should be the segment with lowest 'start'") {
    val segs = make
    segs.add(Segment(16, 5))
    segs.add(Segment(2, 4))
    segs.add(Segment(12, 1))
    assertEquals(segs.firstSegment(), Segment(2, 4))
  }
  test("firstSegment should be correct even if the first element has been removed") {
    val segs = make
    segs.add(Segment(16, 5))
    segs.add(Segment(2, 4))
    segs.add(Segment(12, 1))
    segs.remove(Segment(2, 2))
    assertEquals(segs.firstSegment(), Segment(4, 2))
  }

  test("lastSegment should be the segment with highest 'start'") {
    val segs = make
    segs.add(Segment(16, 5))
    segs.add(Segment(2, 4))
    segs.add(Segment(12, 1))
    assertEquals(segs.lastSegment(), Segment(16, 5))
  }
  test("lastSegment should be correct even if the last element has been removed") {
    val segs = make
    segs.add(Segment(16, 5))
    segs.add(Segment(2, 4))
    segs.add(Segment(12, 1))
    segs.remove(Segment(18, 3))
    assertEquals(segs.lastSegment(), Segment(16, 2))
  }

  test("iterator should be empty if there are no segments") {
    assert(make.iterator.isEmpty)
  }
  test("iterator should return all the segments in order") {
    val segs = make
    segs.add(Segment(16, 5))
    segs.add(Segment(2, 4))
    segs.add(Segment(12, 1))
    segs.remove(Segment(18, 3))

    assertEquals(segs.iterator.toSeq, Seq(Segment(2, 4), Segment(12, 1), Segment(16, 2)))
  }

  def make: ChunkSegs = new ChunkSegs
}
