package hexacraft.rs

import munit.FunSuite

class RustLibTest extends FunSuite {
  test("hello") {
    assertEquals(RustLib.hello(), "Hello from Rust!")
  }
}
