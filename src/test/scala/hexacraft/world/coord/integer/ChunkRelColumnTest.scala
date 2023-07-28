package hexacraft.world.coord.integer

import munit.FunSuite

class ChunkRelColumnTest extends FunSuite {
  test("Y can use the entire range") {
    assertEquals(ChunkRelColumn.create(-14).Y, -14)
    assertEquals(ChunkRelColumn.create(0x7ff).Y, 0x7ff)
    assertEquals(ChunkRelColumn.create(0x800).Y, -0x800)
    assertEquals(ChunkRelColumn.create(0xfff).Y, -1)
    assertEquals(ChunkRelColumn.create(0x1000).Y, 0)
  }

  test("value should be in YYY-format and correct") {
    assertEquals(ChunkRelColumn.create(-14).value, -14 & 0xfff)
    assertEquals(ChunkRelColumn.create(0x7ff).value, 0x7ff)
    assertEquals(ChunkRelColumn.create(0x800).value, -0x800 & 0xfff)
    assertEquals(ChunkRelColumn.create(0xfff).value, -1 & 0xfff)
    assertEquals(ChunkRelColumn.create(0x1000).value, 0)
  }
}
