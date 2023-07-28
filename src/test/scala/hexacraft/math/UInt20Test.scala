package hexacraft.math

import munit.FunSuite

class UInt20Test extends FunSuite {
  test("the entire 20-bit range can be used") {
    assertEquals(UInt20(-14).toInt, -14 & 0xfffff)
    assertEquals(UInt20(0x7ffff).toInt, 0x7ffff)
    assertEquals(UInt20(0x80000).toInt, 0x80000)
    assertEquals(UInt20(0xfffff).toInt, 0xfffff)
  }

  test("UInt20.apply wraps after 20 bits") {
    assertEquals(UInt20(0x100000), UInt20(0))
    assertEquals(UInt20(0x123456), UInt20(0x23456))
    assertEquals(UInt20(0x12345678), UInt20(0x45678))
  }
}
