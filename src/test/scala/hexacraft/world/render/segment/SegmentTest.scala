package hexacraft.world.render.segment

import munit.FunSuite

class SegmentTest extends FunSuite {
  test("Segment should fail for negative 'start'") {
    Segment(4, 5)
    Segment(0, 5)
    intercept[IllegalArgumentException](Segment(-2, 3))
    intercept[IllegalArgumentException](Segment(-2, 1))
  }
  test("Segment should fail for non-positive 'length'") {
    intercept[IllegalArgumentException](Segment(3, -2))
    intercept[IllegalArgumentException](Segment(0, -10))
    intercept[IllegalArgumentException](Segment(5, 0))
  }

  test("contains should return false for segments completely to the left") {
    assert(!Segment(5, 3).contains(Segment(2, 3)))
  }
  test("contains should return false for segments completely to the right") {
    assert(!Segment(5, 3).contains(Segment(8, 3)))
  }
  test("contains should return false for segments partially to the left") {
    assert(!Segment(5, 3).contains(Segment(4, 3)))
  }
  test("contains should return false for segments partially to the right") {
    assert(!Segment(5, 3).contains(Segment(7, 3)))
  }
  test("contains should return false for segments containing this one") {
    assert(!Segment(5, 3).contains(Segment(4, 7)))
  }
  test("contains should return true for segments contained in this one") {
    assert(Segment(5, 3).contains(Segment(5, 3)))
    assert(Segment(5, 3).contains(Segment(5, 2)))
    assert(Segment(5, 3).contains(Segment(6, 2)))
  }

  test("overlaps should return false for segments completely to the left") {
    assert(!Segment(5, 3).overlaps(Segment(2, 3)))
  }
  test("overlaps should return false for segments completely to the right") {
    assert(!Segment(5, 3).overlaps(Segment(8, 3)))
  }
  test("overlaps should return true for segments partially to the left") {
    assert(Segment(5, 3).overlaps(Segment(4, 3)))
  }
  test("overlaps should return true for segments partially to the right") {
    assert(Segment(5, 3).overlaps(Segment(7, 3)))
  }
  test("overlaps should return true for segments containing this one") {
    assert(Segment(5, 3).overlaps(Segment(4, 7)))
  }
  test("overlaps should return true for segments contained in this one") {
    assert(Segment(5, 3).overlaps(Segment(5, 3)))
    assert(Segment(5, 3).overlaps(Segment(5, 2)))
    assert(Segment(5, 3).overlaps(Segment(6, 2)))
  }
}
