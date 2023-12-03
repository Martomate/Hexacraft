package hexacraft.util

import munit.FunSuite

class PointerWrapperTest extends FunSuite {
  test("ints should apply the function") {
    val pw = new PointerWrapper
    var applied = false
    pw.ints((_, _) => applied = true)
    assert(applied)
  }

  test("ints should pass single-element arrays as parameters") {
    val pw = new PointerWrapper
    pw.ints { (px, py) =>
      assertEquals(px.length, 1)
      assertEquals(py.length, 1)
    }
  }

  test("ints should return the first values of the arrays") {
    val pw = new PointerWrapper
    val (rx, ry) = pw.ints { (px, py) =>
      px(0) = 7
      py(0) = 4
    }
    assertEquals(rx, 7)
    assertEquals(ry, 4)
  }

  test("doubles should apply the function") {
    val pw = new PointerWrapper
    var applied = false
    pw.doubles((_, _) => applied = true)
    assert(applied)
  }

  test("doubles should pass single-element arrays as parameters") {
    val pw = new PointerWrapper
    pw.doubles { (px, py) =>
      assertEquals(px.length, 1)
      assertEquals(py.length, 1)
    }
  }

  test("doubles should return the first values of the arrays") {
    val pw = new PointerWrapper
    val (rx, ry) = pw.doubles { (px, py) =>
      px(0) = 7.3
      py(0) = 4.2
    }
    assertEquals(rx, 7.3)
    assertEquals(ry, 4.2)
  }
}
