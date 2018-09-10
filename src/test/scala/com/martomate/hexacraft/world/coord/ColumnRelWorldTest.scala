package com.martomate.hexacraft.world.coord

import com.martomate.hexacraft.world.CylinderSize
import com.martomate.hexacraft.world.coord.integer.ColumnRelWorld
import org.scalatest.FunSuite

class ColumnRelWorldTest extends FunSuite {
  test("X is correct in entire range") {
    assertResult(     -14)(ColumnRelWorld(     -14, 17, size).X)
    assertResult( 0x7ffff)(ColumnRelWorld( 0x7ffff, 17, size).X)
    assertResult(-0x80000)(ColumnRelWorld( 0x80000, 17, size).X)
    assertResult(      -1)(ColumnRelWorld( 0xfffff, 17, size).X)
    assertResult(       0)(ColumnRelWorld(0x100000, 17, size).X)
  }

  test("Z is correct in entire range") {
    assertResult( 1)(ColumnRelWorld(532,  1, size).Z)
    assertResult(15)(ColumnRelWorld(532, -1, size).Z)
    assertResult( 0)(ColumnRelWorld(532, 16, size).Z)
    assertResult( 0)(ColumnRelWorld(532, 16 * 25235, size).Z)
  }

  test("value is in XXXXXZZZZZYYY-format and correct") {
    assertResult(532L << 20 | (17L & 15))(ColumnRelWorld(532, 17, size).value)
    assertResult((-532L & 0xfffff) << 20 | (17L & 15))(ColumnRelWorld(-532, 17, size).value)
    assertResult(532L << 20 | (-17L & 15))(ColumnRelWorld(532, -17, size).value)
  }
  
  private def size = new CylinderSize(4)
}
