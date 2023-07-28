package hexacraft.world.coord.integer

import hexacraft.math.{Int12, Int20}
import hexacraft.world.CylinderSize

import munit.FunSuite

class ChunkRelWorldTest extends FunSuite {
  private val cylSize = CylinderSize(4)
  import cylSize.impl

  test("Y should be correct in entire range") {
    assertEquals(ChunkRelWorld(532, -14, 17).Y, Int12(-14))
    assertEquals(ChunkRelWorld(532, 0x7ff, 17).Y, Int12(0x7ff))
    assertEquals(ChunkRelWorld(532, 0x800, 17).Y, Int12(-0x800))
    assertEquals(ChunkRelWorld(532, 0xfff, 17).Y, Int12(-1))
    assertEquals(ChunkRelWorld(532, 0x1000, 17).Y, Int12(0))
  }

  test("X should be correct in entire range") {
    assertEquals(ChunkRelWorld(-14, -14, 17).X, Int20(-14))
    assertEquals(ChunkRelWorld(0x7ffff, -14, 17).X, Int20(0x7ffff))
    assertEquals(ChunkRelWorld(0x80000, -14, 17).X, Int20(-0x80000))
    assertEquals(ChunkRelWorld(0xfffff, -14, 17).X, Int20(-1))
    assertEquals(ChunkRelWorld(0x100000, -14, 17).X, Int20(0))
  }

  test("Z should be correct in entire range") {
    assertEquals(ChunkRelWorld(532, -14, 1).Z, Int20(1))
    assertEquals(ChunkRelWorld(532, -14, -1).Z, Int20(15))
    assertEquals(ChunkRelWorld(532, -14, 16).Z, Int20(0))
    assertEquals(ChunkRelWorld(532, -14, 16 * 25235).Z, Int20(0))
  }

  test("offset should give the correct result") {
    val c = ChunkRelWorld(532, -14, 17).offset(-4, 71, -12345)
    assertEquals(c.X, Int20(532 - 4))
    assertEquals(c.Y, Int12(-14 + 71))
    assertEquals(c.Z, Int20(8))
  }

  test("value should be in XXXXXZZZZZYYY-format and correct") {
    assertEquals(ChunkRelWorld(532, -14, 17).value, 532L << 32 | (17L & 15) << 12 | (-14 & 0xfff))
    assertEquals(ChunkRelWorld(-532, -14, 17).value, (-532L & 0xfffff) << 32 | (17L & 15) << 12 | (-14 & 0xfff))
    assertEquals(ChunkRelWorld(532, -14, -17).value, 532L << 32 | (-17L & 15) << 12 | (-14 & 0xfff))
  }

  test("withBlockCoords should give the correct result") {
    var c = BlockRelWorld(0, 0, 0, ChunkRelWorld(532, -14, 17))
    assertEquals(c.x, 532 * 16)
    assertEquals(c.y, -14 * 16)
    assertEquals(c.z, 1 * 16)

    c = BlockRelWorld(-4, 71, -12345, ChunkRelWorld(532, -14, 17))
    assertEquals(c.x, 532 * 16 - 4)
    assertEquals(c.y, -14 * 16 + 71)
    assertEquals(c.z, (17 * 16 - 12345) & cylSize.totalSizeMask)
  }
}
