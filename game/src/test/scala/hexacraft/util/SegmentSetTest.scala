package hexacraft.util

import munit.FunSuite

class SegmentSetTest extends FunSuite {
  test("add should add the segment if it doesn't exist") {
    val set = make
    val seg = Segment(5, 8)
    set.add(seg)
    assertEquals(set.toSeq, Seq(seg))
  }
  test("add should complain if it already exists") {
    val set = make
    val seg = Segment(5, 8)
    set.add(seg)
    intercept[IllegalArgumentException](set.add(seg))
  }
  test("add should complain if it overlaps an existing segment") {
    val set = make
    set.add(Segment(5, 8))
    intercept[IllegalArgumentException](set.add(Segment(6, 4)))
    intercept[IllegalArgumentException](set.add(Segment(6, 12)))
    intercept[IllegalArgumentException](set.add(Segment(2, 4)))
    intercept[IllegalArgumentException](set.add(Segment(2, 12)))
  }
  test("add should not complain if it doesn't overlap an existing segment") {
    val set = make
    set.add(Segment(5, 8))
    set.add(Segment(2, 3))
    set.add(Segment(13, 3))
  }

  test("remove should remove the segment if it exists, and return true") {
    val set = make
    set.add(Segment(6, 23))
    assert(set.remove(Segment(6, 23)))
    assert(set.toSeq.isEmpty)
  }
  test("remove should not remove it if it doesn't exist") {
    val set = make
    set.add(Segment(2, 3))
    assert(!set.remove(Segment(6, 23)))
    assertEquals(set.toSeq, Seq(Segment(2, 3)))
  }
  test("remove should remove this part of an existing segment that contains this") {
    val set = make
    set.add(Segment(2, 43))
    assert(set.remove(Segment(6, 23)))
    assertEquals(set.toSeq, Seq(Segment(2, 4), Segment(2 + 4 + 23, 43 - 4 - 23)))
  }
  test("remove should not remove it if there is only partial overlap") {
    val set = make
    set.add(Segment(2, 13))
    assert(!set.remove(Segment(6, 23)))
    assertEquals(set.toSeq, Seq(Segment(2, 13)))
  }

  test("totalLength should be 0 for a new ChunkSegs") {
    assertEquals(make.totalLength, 0)
  }
  test("totalLength should be the sum of the lengths of the segments") {
    val set = make
    set.add(Segment(16, 5))
    set.add(Segment(2, 4))
    set.add(Segment(12, 1))
    assertEquals(set.totalLength, 5 + 4 + 1)
  }

  test("firstSegment should be the segment with lowest 'start'") {
    val set = make
    set.add(Segment(16, 5))
    set.add(Segment(2, 4))
    set.add(Segment(12, 1))
    assertEquals(set.firstSegment(), Segment(2, 4))
  }
  test("firstSegment should be correct even if the first element has been removed") {
    val set = make
    set.add(Segment(16, 5))
    set.add(Segment(2, 4))
    set.add(Segment(12, 1))
    set.remove(Segment(2, 2))
    assertEquals(set.firstSegment(), Segment(4, 2))
  }

  test("lastSegment should be the segment with highest 'start'") {
    val set = make
    set.add(Segment(16, 5))
    set.add(Segment(2, 4))
    set.add(Segment(12, 1))
    assertEquals(set.lastSegment(), Segment(16, 5))
  }
  test("lastSegment should be correct even if the last element has been removed") {
    val set = make
    set.add(Segment(16, 5))
    set.add(Segment(2, 4))
    set.add(Segment(12, 1))
    set.remove(Segment(18, 3))
    assertEquals(set.lastSegment(), Segment(16, 2))
  }

  test("iterator should be empty if there are no segments") {
    assert(make.iterator.isEmpty)
  }
  test("iterator should return all the segments in order") {
    val set = make
    set.add(Segment(16, 5))
    set.add(Segment(2, 4))
    set.add(Segment(12, 1))
    set.remove(Segment(18, 3))

    assertEquals(set.iterator.toSeq, Seq(Segment(2, 4), Segment(12, 1), Segment(16, 2)))
  }

  def make: SegmentSet = new SegmentSet
}
