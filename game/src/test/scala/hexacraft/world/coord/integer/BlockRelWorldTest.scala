package hexacraft.world.coord.integer

import hexacraft.math.bits.{Int12, Int20}
import hexacraft.world.CylinderSize

import munit.FunSuite

class BlockRelWorldTest extends FunSuite {
  given cylSize: CylinderSize = CylinderSize(4)

  test("chunk xyz should be correct in normal range") {
    val c = BlockRelWorld(532, -14, 17, 3, 7, 5)
    assertEquals(c.cx, 3.toByte)
    assertEquals(c.cy, 7.toByte)
    assertEquals(c.cz, 5.toByte)
  }

  test("chunk xyz should be correct outside normal range") {
    val c = BlockRelWorld(532, -14, 17, 3 + 5 * 16, 7 - 3 * 16, 5 + 200 * 16)
    assertEquals(c.cx, 3.toByte)
    assertEquals(c.cy, 7.toByte)
    assertEquals(c.cz, 5.toByte)
  }

  test("Y should be correct in entire range") {
    assertEquals(BlockRelWorld(532, -14, 17, 3, 7, 5).Y, Int12(-14))
    assertEquals(BlockRelWorld(532, 0x7ff, 17, 3, 7, 5).Y, Int12(0x7ff))
    assertEquals(BlockRelWorld(532, 0x800, 17, 3, 7, 5).Y, Int12(-0x800))
    assertEquals(BlockRelWorld(532, 0xfff, 17, 3, 7, 5).Y, Int12(-1))
    assertEquals(BlockRelWorld(532, 0x1000, 17, 3, 7, 5).Y, Int12(0))
  }

  test("X should be correct in entire range") {
    assertEquals(BlockRelWorld(-14, -14, 17, 3, 7, 5).X, Int20(-14))
    assertEquals(BlockRelWorld(0x7ffff, -14, 17, 3, 7, 5).X, Int20(0x7ffff))
    assertEquals(BlockRelWorld(0x80000, -14, 17, 3, 7, 5).X, Int20(-0x80000))
    assertEquals(BlockRelWorld(0xfffff, -14, 17, 3, 7, 5).X, Int20(-1))
    assertEquals(BlockRelWorld(0x100000, -14, 17, 3, 7, 5).X, Int20(0))
  }

  test("Z should be correct in entire range") {
    assertEquals(BlockRelWorld(532, -14, 1, 3, 7, 5).Z, Int20(1))
    assertEquals(BlockRelWorld(532, -14, -1, 3, 7, 5).Z, Int20(15))
    assertEquals(BlockRelWorld(532, -14, 16, 3, 7, 5).Z, Int20(0))
    assertEquals(BlockRelWorld(532, -14, 16 * 25235, 3, 7, 5).Z, Int20(0))
  }

  test("xyz should be correct in normal range") {
    val c = BlockRelWorld(532, -14, 17, 3, 7, 5)
    assertEquals(c.x, 532 * 16 + 3)
    assertEquals(c.y, -14 * 16 + 7)
    assertEquals(c.z, 1 * 16 + 5)
  }

  test("convenient constructor should give correct result") {
    val c = BlockRelWorld(532 * 16 + 3, -14 * 16 + 7, -15 * 16 + 5)
    assertEquals(c.x, 532 * 16 + 3)
    assertEquals(c.y, -14 * 16 + 7)
    assertEquals(c.z, 1 * 16 + 5)
  }

  test("offset should give correct result") {
    val c = BlockRelWorld(532, -14, 17, 3, 7, 5).offset(-4, 71, -12345)
    assertEquals(c.x, 532 * 16 - 1)
    assertEquals(c.y, -14 * 16 + 78)
    assertEquals(c.z, 13 * 16 + 12)
  }

  test("value should be in XXXXXZZZZZYYYxyz-format and correct") {
    assertEquals(
      BlockRelWorld(532, -14, 17, 3, 7, 5).value,
      532L << 44 | (17L & 15) << 24 | (-14 & 0xfff) << 12 | 3 << 8 | 7 << 4 | 5
    )
    assertEquals(
      BlockRelWorld(-532, -14, 17, 3, 7, 5).value,
      (-532L & 0xfffff) << 44 | (17L & 15) << 24 | (-14 & 0xfff) << 12 | 3 << 8 | 7 << 4 | 5
    )
    assertEquals(
      BlockRelWorld(532, -14, -17, 3, 7, 5).value,
      532L << 44 | (-17L & 15) << 24 | (-14 & 0xfff) << 12 | 3 << 8 | 7 << 4 | 5
    )
  }
}
