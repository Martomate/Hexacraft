package hexacraft.util

import munit.FunSuite

class LoopTest extends FunSuite {
  test("loop should iterate correctly") {
    var acc = 0
    Loop.rangeUntil(3, 7) { i =>
      acc += i
    }
    assertEquals(acc, 3 + 4 + 5 + 6)
  }
}
