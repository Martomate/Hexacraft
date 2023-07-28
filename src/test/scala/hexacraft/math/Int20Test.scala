package hexacraft.math

import munit.FunSuite

class Int20Test extends FunSuite {
  test("the entire 20-bit range can be used") {
    assertEquals(Int20(-14).toInt, -14)
    assertEquals(Int20(0x7ffff).toInt, 0x7ffff)
    assertEquals(Int20(0x80000).toInt, -0x80000)
    assertEquals(Int20(0xfffff).toInt, -1)
  }

  test("Int20.apply wraps after 20 bits") {
    assertEquals(Int20(0x100000), Int20(0))
    assertEquals(Int20(0x123456), Int20(0x23456))
    assertEquals(Int20(0x12345678), Int20(0x45678))
  }

  test("Int20.repr returns the underlying 20-bit unsigned int") {
    assertEquals(Int20(-14).repr, UInt20(-14 & 0xfffff))
    assertEquals(Int20(0x7ffff).repr, UInt20(0x7ffff))
    assertEquals(Int20(0x80000).repr, UInt20(0x80000))
    assertEquals(Int20(0xfffff).repr, UInt20(0xfffff))
    assertEquals(Int20(0x100000).repr, UInt20(0))
  }
}
