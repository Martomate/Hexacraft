package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize
import org.scalatest.FunSuite

class ChunkRelColumnTest extends FunSuite {
  private val size = new CylinderSize(4)
  import size.impl

  test("Y can use the entire range") {
    assertResult(   -14)(ChunkRelColumn(   -14).Y)
    assertResult( 0x7ff)(ChunkRelColumn( 0x7ff).Y)
    assertResult(-0x800)(ChunkRelColumn( 0x800).Y)
    assertResult(    -1)(ChunkRelColumn( 0xfff).Y)
    assertResult(     0)(ChunkRelColumn(0x1000).Y)
  }

  test("value is in YYY-format and correct") {
    assertResult(   -14 & 0xfff)(ChunkRelColumn(   -14).value)
    assertResult( 0x7ff)(ChunkRelColumn( 0x7ff).value)
    assertResult(-0x800 & 0xfff)(ChunkRelColumn( 0x800).value)
    assertResult(    -1 & 0xfff)(ChunkRelColumn( 0xfff).value)
    assertResult(     0)(ChunkRelColumn(0x1000).value)
  }
}
