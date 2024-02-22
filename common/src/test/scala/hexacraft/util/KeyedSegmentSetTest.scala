package hexacraft.util

class KeyedSegmentSetTest extends SegmentSetTest {
  test("lastKeyAndSegment should return the correct key if nothing is removed") {
    val set = make
    set.add(12, Segment(5, 6))
    set.add(23, Segment(2, 2))
    assertEquals(set.lastKeyAndSegment, 12 -> Segment(5, 6))
  }
  test("lastKeyAndSegment should return the correct key when something is removed in the beginning") {
    val set = make
    set.add(23, Segment(2, 2))
    set.add(12, Segment(5, 6))
    set.remove(23, Segment(2, 2))
    assertEquals(set.lastKeyAndSegment, 12 -> Segment(5, 6))
  }
  test("lastKeyAndSegment should return the correct key when something is removed in the end") {
    val set = make
    set.add(23, Segment(2, 2))
    set.add(12, Segment(5, 6))
    set.remove(12, Segment(5, 6))
    assertEquals(set.lastKeyAndSegment, 23 -> Segment(2, 2))
  }
  test("lastKeyAndSegment should return the correct key when something is changed in the end") {
    val set = make
    set.add(23, Segment(2, 2))
    set.add(12, Segment(5, 6))
    set.remove(12, Segment(5, 6))
    set.add(23, Segment(5, 6))
    assertEquals(set.lastKeyAndSegment, 23 -> Segment(5, 6))
  }
  test("lastKeyAndSegment should return the correct key after several removals in the end") {
    val set = make
    set.add(23, Segment(2, 2))
    set.add(12, Segment(45, 6))
    set.add(12, Segment(35, 6))
    set.add(12, Segment(25, 6))
    set.add(12, Segment(5, 6))
    set.remove(12, Segment(35, 6))
    set.remove(12, Segment(25, 6))
    set.remove(12, Segment(45, 6))
    assertEquals(set.lastKeyAndSegment, 12 -> Segment(5, 6))
  }
  test("lastKeyAndSegment should return the correct key when part of the last segment is removed") {
    val set = make
    set.add(23, Segment(2, 2))
    set.add(12, Segment(5, 6))
    set.remove(12, Segment(5, 2))
    assertEquals(set.lastKeyAndSegment, 12 -> Segment(7, 4))
    set.remove(12, Segment(8, 3))
    assertEquals(set.lastKeyAndSegment, 12 -> Segment(7, 1))
  }

  test("add(T, Segment) should complain if the segment already exists, regardless of key") {
    val set = make
    set.add(3, Segment(4, 6))
    intercept[IllegalArgumentException](set.add(4, Segment(4, 6)))
    intercept[IllegalArgumentException](set.add(3, Segment(4, 6)))
    intercept[IllegalArgumentException](set.add(3, Segment(3, 3)))
  }

  test("remove(T, Segment) should complain if the segment is not part of an existing segment") {
    val set = make
    set.add(3, Segment(4, 6))
    intercept[IllegalArgumentException](set.remove(3, Segment(3, 6)))
    intercept[IllegalArgumentException](set.remove(3, Segment(5, 6)))
    intercept[IllegalArgumentException](set.remove(3, Segment(3, 8)))
    intercept[IllegalArgumentException](set.remove(3, Segment(23, 3)))
  }
  test("remove(T, Segment) should complain if the segment exists, but with another key") {
    val set = make
    set.add(3, Segment(4, 6))
    intercept[IllegalArgumentException](set.remove(4, Segment(4, 6)))
  }
  test("remove(T, Segment) should not remove anything if it fails") {
    val set = make
    set.add(3, Segment(4, 6))
    intercept[IllegalArgumentException](set.remove(4, Segment(6, 2)))
    assertEquals(set.toSeq, Seq(Segment(4, 6)))
  }

  override def make: KeyedSegmentSet[Int] = new KeyedSegmentSet
}
