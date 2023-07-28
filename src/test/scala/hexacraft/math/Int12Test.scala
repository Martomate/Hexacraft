package hexacraft.math

import munit.FunSuite

class Int12Test extends FunSuite {
  test("the entire 12-bit range can be used") {
    assertEquals(Int12(-14).toInt, -14)
    assertEquals(Int12(0x7ff).toInt, 0x7ff)
    assertEquals(Int12(0x800).toInt, -0x800)
    assertEquals(Int12(0xfff).toInt, -1)
  }

  test("Int12.apply wraps after 12 bits") {
    assertEquals(Int12(0x1000), Int12(0))
    assertEquals(Int12(0x1234), Int12(0x234))
    assertEquals(Int12(0x12345678), Int12(0x678))
  }

  test("Int12.repr returns the underlying 12-bit unsigned int") {
    assertEquals(Int12(-14).repr, UInt12(-14 & 0xfff))
    assertEquals(Int12(0x7ff).repr, UInt12(0x7ff))
    assertEquals(Int12(0x800).repr, UInt12(0x800))
    assertEquals(Int12(0xfff).repr, UInt12(0xfff))
    assertEquals(Int12(0x1000).repr, UInt12(0))
  }
}
