package hexacraft.world.coord.integer

import hexacraft.math.bits.Int20
import hexacraft.world.CylinderSize

import munit.FunSuite

class ColumnRelWorldTest extends FunSuite {
  private val cylSize = CylinderSize(4)
  import cylSize.impl

  test("X should be correct in entire range") {
    assertEquals(ColumnRelWorld(-14, 17).X, Int20(-14))
    assertEquals(ColumnRelWorld(0x7ffff, 17).X, Int20(0x7ffff))
    assertEquals(ColumnRelWorld(0x80000, 17).X, Int20(-0x80000))
    assertEquals(ColumnRelWorld(0xfffff, 17).X, Int20(-1))
    assertEquals(ColumnRelWorld(0x100000, 17).X, Int20(0))
  }

  test("Z should be correct in entire range") {
    assertEquals(ColumnRelWorld(532, 1).Z, Int20(1))
    assertEquals(ColumnRelWorld(532, -1).Z, Int20(15))
    assertEquals(ColumnRelWorld(532, 16).Z, Int20(0))
    assertEquals(ColumnRelWorld(532, 16 * 25235).Z, Int20(0))
  }

  test("value should be in XXXXXZZZZZYYY-format and correct") {
    assertEquals(ColumnRelWorld(532, 17).value, 532L << 20 | (17L & 15))
    assertEquals(ColumnRelWorld(-532, 17).value, (-532L & 0xfffff) << 20 | (17L & 15))
    assertEquals(ColumnRelWorld(532, -17).value, 532L << 20 | (-17L & 15))
  }
}
