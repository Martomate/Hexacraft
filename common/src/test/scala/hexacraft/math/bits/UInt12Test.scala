package hexacraft.math.bits

import hexacraft.math.bits.UInt12
import munit.FunSuite

class UInt12Test extends FunSuite {
  test("the entire 12-bit range can be used") {
    assertEquals(UInt12(-14).toInt, -14 & 0xfff)
    assertEquals(UInt12(0x7ff).toInt, 0x7ff)
    assertEquals(UInt12(0x800).toInt, 0x800)
    assertEquals(UInt12(0xfff).toInt, 0xfff)
  }

  test("UInt12.apply wraps after 12 bits") {
    assertEquals(UInt12(0x1000), UInt12(0))
    assertEquals(UInt12(0x1234), UInt12(0x234))
    assertEquals(UInt12(0x12345678), UInt12(0x678))
  }
}
