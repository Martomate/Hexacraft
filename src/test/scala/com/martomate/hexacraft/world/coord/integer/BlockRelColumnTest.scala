package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize
import org.scalatest.FunSuite

class BlockRelColumnTest extends FunSuite {
  private val size = new CylinderSize(4)
  import size.impl

  test("xyz is correct in normal range") {
    val c = BlockRelColumn(-14, 3, 7, 5)
    assertResult(3)(c.cx)
    assertResult(7)(c.cy)
    assertResult(5)(c.cz)
  }

  test("xyz is correct outside normal range") {
    val c = BlockRelColumn(-14, 3 + 5*16, 7 - 3*16, 5 + 200 * 16)
    assertResult(3)(c.cx)
    assertResult(7)(c.cy)
    assertResult(5)(c.cz)
  }

  test("Y is correct in entire range") {
    assertResult(   -14)(BlockRelColumn(   -14, 3, 7, 5).Y)
    assertResult( 0x7ff)(BlockRelColumn( 0x7ff, 3, 7, 5).Y)
    assertResult(-0x800)(BlockRelColumn( 0x800, 3, 7, 5).Y)
    assertResult(    -1)(BlockRelColumn( 0xfff, 3, 7, 5).Y)
    assertResult(     0)(BlockRelColumn(0x1000, 3, 7, 5).Y)
  }

  test("value is in YYYxyz-format and correct") {
    val c = BlockRelColumn(-14, 3, 7, 5)
    assertResult((-14 & 0xfff) << 12 | 3 << 8 | 7 << 4 | 5)(c.value)
  }
}
