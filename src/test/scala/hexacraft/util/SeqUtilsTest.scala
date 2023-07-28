package hexacraft.util

import munit.FunSuite

class SeqUtilsTest extends FunSuite {
  test("whileSome should do a maximum of maxCount iterations") {
    var total = 0
    SeqUtils.whileSome(10, Some(4)) { t =>
      total += t
    }
    assertEquals(total, 40)
  }
  test("whileSome should stop if maker == None") {
    var total = 0
    SeqUtils.whileSome(10, if (total < 32) Some(4) else None) { t =>
      total += t
    }
    assertEquals(total, 32)
  }
}
