package hexacraft.util

import munit.FunSuite

// TODO: improve these tests so they test more relevant cases
class DenseKeyedSegmentStackTest extends FunSuite {
  test("hasMapping should be false if the chunk has never had a mapping") {
    val handler = new DenseKeyedSegmentStack[String]
    assert(!handler.hasMapping("a"))
  }
  test("hasMapping should be false if only other chunks have had mappings") {
    val handler = new DenseKeyedSegmentStack[String]
    handler.push("b", 3)

    assert(!handler.hasMapping("a"))
  }
  test("hasMapping should be true if the chunk has a segment in this handler") {
    val handler = new DenseKeyedSegmentStack[String]
    handler.push("a", 3)

    assert(handler.hasMapping("a"))
  }
  test("hasMapping should be false if all the chunk's segments have been removed") {
    val handler = new DenseKeyedSegmentStack[String]
    val s = handler.push("a", 3)
    handler.pop("a", s)

    assert(!handler.hasMapping("a"))
  }

  test("length should be 0 in the beginning") {
    assertEquals((new DenseKeyedSegmentStack).length, 0)
  }
  test("length should be equal to the maximum size Segment(0, size) to contain all the segments") {
    val handler = new DenseKeyedSegmentStack[String]
    handler.push("a", 5)
    handler.push("a", 6)

    assertEquals(handler.length, 5 + 6)
  }
  test("length should remain correct even after removals at the end") {
    val handler = new DenseKeyedSegmentStack[String]
    handler.push("a", 2)
    handler.push("a", 3)
    handler.push("a", 6)
    handler.pop("a", Segment(5, 6))
    handler.pop("a", Segment(2, 3))

    assertEquals(handler.length, 2)
  }

  test("totalLengthForChunk should be 0 in the beginning") {
    assertEquals((new DenseKeyedSegmentStack).totalLengthForChunk("a"), 0)
  }
  test("totalLengthForChunk should be 0 for chunks without mapping") {
    val handler = new DenseKeyedSegmentStack[String]
    handler.push("a", 3)

    assertEquals(handler.totalLengthForChunk("b"), 0)
  }
  test("totalLengthForChunk should not count the holes of a chunk's segments") {
    val handler = new DenseKeyedSegmentStack[String]
    handler.push("d", 5)
    handler.push("a", 3)
    handler.push("d", 7)
    handler.push("a", 3)

    assertEquals(handler.totalLengthForChunk("a"), 6)
  }

  test("pop should remove the segment if it exists among the segments of this chunk") {
    val handler = new DenseKeyedSegmentStack[String]
    handler.push("d", 5)
    handler.push("a", 3)

    handler.pop("a", Segment(5, 3))
    assert(handler.segments("a").toSeq.isEmpty)
  }
  test("pop should complain if the chunk doesn't have a matching segment") {
    val handler = new DenseKeyedSegmentStack[String]
    handler.push("d", 5)
    handler.push("a", 3)

    intercept[IllegalArgumentException](handler.pop("a", Segment(16, 5)))
    intercept[IllegalArgumentException](handler.pop("a", Segment(5, 5)))
    assertEquals(handler.segments("a").toSeq, Seq(Segment(5, 3)))
  }
  test("pop should complain if there is a matching segment but on the wrong chunk") {
    val handler = new DenseKeyedSegmentStack[String]
    handler.push("d", 5)
    handler.push("a", 3)

    intercept[IllegalArgumentException](handler.pop("b", Segment(5, 3)))
    intercept[IllegalArgumentException](handler.pop("c", Segment(6, 1)))
    assertEquals(handler.segments("a").toSeq, Seq(Segment(5, 3)))
  }

  test("segments should be empty if the chunk doesn't have a mapping") {
    val handler = new DenseKeyedSegmentStack[String]
    handler.push("d", 2)
    handler.push("a", 1)

    assert(handler.segments("c").isEmpty)
  }
  test("segments should be correct if the chunk has a mapping") {
    val handler = new DenseKeyedSegmentStack[String]
    handler.push("B", 2)
    handler.push("A", 1)
    handler.push("B", 9)
    handler.push("A", 11)
    handler.push("B", 9)
    handler.push("A", 4)

    assertEquals(handler.segments("A").toSeq, Seq(Segment(2, 1), Segment(12, 11), Segment(32, 4)))
  }

  test("lastSegment should be None if there are no segments") {
    assertEquals((new DenseKeyedSegmentStack).lastSegment, None)
  }
  test("lastSegment should work if nothing is removed") {
    val handler = new DenseKeyedSegmentStack[String]
    handler.push("d", 2)
    handler.push("a", 1)

    assertEquals(handler.lastSegment, Some(("a", Segment(2, 1))))
    assertEquals(handler.lastSegment, Some(("a", Segment(2, 1))))

    handler.push("d", 4)

    assertEquals(handler.lastSegment, Some(("d", Segment(3, 4))))
  }
  test("lastSegment should work if something is removed") {
    val handler = new DenseKeyedSegmentStack[String]
    handler.push("a", 22)
    handler.push("b", 10)
    handler.push("a", 4)

    assertEquals(handler.lastSegment, Some(("a", Segment(32, 4))))

    handler.pop("a", Segment(32, 4))

    assertEquals(handler.lastSegment, Some(("b", Segment(22, 10))))
  }
  test("lastSegment should work if something is removed and added with another chunk") {
    val handler = new DenseKeyedSegmentStack[String]
    handler.push("a", 24)
    handler.relabel("a", "b", Segment(12, 4))
    handler.pop("a", Segment(16, 8))

    assertEquals(handler.lastSegment, Some(("b", Segment(12, 4))))
  }
}
