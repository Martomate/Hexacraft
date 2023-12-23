package hexacraft.world.render

import hexacraft.world.render.{ChunkSegsWithKey, Segment}

class ChunkSegsWithKeyTest extends ChunkSegsTest {
  test("lastKeyAndSegment should return the correct key if nothing is removed") {
    val segs = make
    segs.add(12, Segment(5, 6))
    segs.add(23, Segment(2, 2))
    assertEquals(segs.lastKeyAndSegment(), 12 -> Segment(5, 6))
  }
  test("lastKeyAndSegment should return the correct key when something is removed in the beginning") {
    val segs = make
    segs.add(23, Segment(2, 2))
    segs.add(12, Segment(5, 6))
    segs.remove(23, Segment(2, 2))
    assertEquals(segs.lastKeyAndSegment(), 12 -> Segment(5, 6))
  }
  test("lastKeyAndSegment should return the correct key when something is removed in the end") {
    val segs = make
    segs.add(23, Segment(2, 2))
    segs.add(12, Segment(5, 6))
    segs.remove(12, Segment(5, 6))
    assertEquals(segs.lastKeyAndSegment(), 23 -> Segment(2, 2))
  }
  test("lastKeyAndSegment should return the correct key when something is changed in the end") {
    val segs = make
    segs.add(23, Segment(2, 2))
    segs.add(12, Segment(5, 6))
    segs.remove(12, Segment(5, 6))
    segs.add(23, Segment(5, 6))
    assertEquals(segs.lastKeyAndSegment(), 23 -> Segment(5, 6))
  }
  test("lastKeyAndSegment should return the correct key after several removals in the end") {
    val segs = make
    segs.add(23, Segment(2, 2))
    segs.add(12, Segment(45, 6))
    segs.add(12, Segment(35, 6))
    segs.add(12, Segment(25, 6))
    segs.add(12, Segment(5, 6))
    segs.remove(12, Segment(35, 6))
    segs.remove(12, Segment(25, 6))
    segs.remove(12, Segment(45, 6))
    assertEquals(segs.lastKeyAndSegment(), 12 -> Segment(5, 6))
  }
  test("lastKeyAndSegment should return the correct key when part of the last segment is removed") {
    val segs = make
    segs.add(23, Segment(2, 2))
    segs.add(12, Segment(5, 6))
    segs.remove(12, Segment(5, 2))
    assertEquals(segs.lastKeyAndSegment(), 12 -> Segment(7, 4))
    segs.remove(12, Segment(8, 3))
    assertEquals(segs.lastKeyAndSegment(), 12 -> Segment(7, 1))
  }

  test("add(T, Segment) should complain if the segment already exists, regardless of key") {
    val segs = make
    segs.add(3, Segment(4, 6))
    intercept[IllegalArgumentException](segs.add(4, Segment(4, 6)))
    intercept[IllegalArgumentException](segs.add(3, Segment(4, 6)))
    intercept[IllegalArgumentException](segs.add(3, Segment(3, 3)))
  }

  test("remove(T, Segment) should complain if the segment is not part of an existing segment") {
    val segs = make
    segs.add(3, Segment(4, 6))
    intercept[IllegalArgumentException](segs.remove(3, Segment(3, 6)))
    intercept[IllegalArgumentException](segs.remove(3, Segment(5, 6)))
    intercept[IllegalArgumentException](segs.remove(3, Segment(3, 8)))
    intercept[IllegalArgumentException](segs.remove(3, Segment(23, 3)))
  }
  test("remove(T, Segment) should complain if the segment exists, but with another key") {
    val segs = make
    segs.add(3, Segment(4, 6))
    intercept[IllegalArgumentException](segs.remove(4, Segment(4, 6)))
  }
  test("remove(T, Segment) should not remove anything if it fails") {
    val segs = make
    segs.add(3, Segment(4, 6))
    intercept[IllegalArgumentException](segs.remove(4, Segment(6, 2)))
    assertEquals(segs.toSeq, Seq(Segment(4, 6)))
  }

  override def make: ChunkSegsWithKey[Int] = new ChunkSegsWithKey
}
