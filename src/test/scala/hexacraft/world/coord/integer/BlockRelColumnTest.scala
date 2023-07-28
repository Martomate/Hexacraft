package hexacraft.world.coord.integer

import munit.FunSuite

class BlockRelColumnTest extends FunSuite {
  test("xyz should be correct in normal range") {
    val c = BlockRelColumn(-14, 3, 7, 5)
    assertEquals(c.cx, 3.toByte)
    assertEquals(c.cy, 7.toByte)
    assertEquals(c.cz, 5.toByte)
  }

  test("xyz should be correct outside normal range") {
    val c = BlockRelColumn(-14, 3 + 5 * 16, 7 - 3 * 16, 5 + 200 * 16)
    assertEquals(c.cx, 3.toByte)
    assertEquals(c.cy, 7.toByte)
    assertEquals(c.cz, 5.toByte)
  }

  test("Y should be correct in entire range") {
    assertEquals(BlockRelColumn(-14, 3, 7, 5).Y, -14)
    assertEquals(BlockRelColumn(0x7ff, 3, 7, 5).Y, 0x7ff)
    assertEquals(BlockRelColumn(0x800, 3, 7, 5).Y, -0x800)
    assertEquals(BlockRelColumn(0xfff, 3, 7, 5).Y, -1)
    assertEquals(BlockRelColumn(0x1000, 3, 7, 5).Y, 0)
  }

  test("value should be in YYYxyz-format and correct") {
    val c = BlockRelColumn(-14, 3, 7, 5)
    assertEquals(c.value, (-14 & 0xfff) << 12 | 3 << 8 | 7 << 4 | 5)
  }
}
