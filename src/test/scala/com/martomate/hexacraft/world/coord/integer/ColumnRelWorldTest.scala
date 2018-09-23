package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize
import org.scalatest.FunSuite

class ColumnRelWorldTest extends FunSuite {
  private val size = new CylinderSize(4)
  import size.impl
  
  test("X is correct in entire range") {
    assertResult(     -14)(ColumnRelWorld(     -14, 17).X)
    assertResult( 0x7ffff)(ColumnRelWorld( 0x7ffff, 17).X)
    assertResult(-0x80000)(ColumnRelWorld( 0x80000, 17).X)
    assertResult(      -1)(ColumnRelWorld( 0xfffff, 17).X)
    assertResult(       0)(ColumnRelWorld(0x100000, 17).X)
  }

  test("Z is correct in entire range") {
    assertResult( 1)(ColumnRelWorld(532,  1).Z)
    assertResult(15)(ColumnRelWorld(532, -1).Z)
    assertResult( 0)(ColumnRelWorld(532, 16).Z)
    assertResult( 0)(ColumnRelWorld(532, 16 * 25235).Z)
  }

  test("value is in XXXXXZZZZZYYY-format and correct") {
    assertResult(532L << 20 | (17L & 15))(ColumnRelWorld(532, 17).value)
    assertResult((-532L & 0xfffff) << 20 | (17L & 15))(ColumnRelWorld(-532, 17).value)
    assertResult(532L << 20 | (-17L & 15))(ColumnRelWorld(532, -17).value)
  }
}
