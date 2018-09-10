package com.martomate.hexacraft.world.coord

import com.martomate.hexacraft.world.CylinderSize
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import org.scalatest.FunSuite

class BlockRelWorldTest extends FunSuite {
  test("chunk xyz is correct in normal range") {
    val c = BlockRelWorld(532, -14, 17, 3, 7, 5, size)
    assertResult(3)(c.cx)
    assertResult(7)(c.cy)
    assertResult(5)(c.cz)
  }

  test("chunk xyz is correct outside normal range") {
    val c = BlockRelWorld(532, -14, 17, 3 + 5*16, 7 - 3*16, 5 + 200 * 16, size)
    assertResult(3)(c.cx)
    assertResult(7)(c.cy)
    assertResult(5)(c.cz)
  }

  test("Y is correct in entire range") {
    assertResult(   -14)(BlockRelWorld(532,    -14, 17, 3, 7, 5, size).Y)
    assertResult( 0x7ff)(BlockRelWorld(532,  0x7ff, 17, 3, 7, 5, size).Y)
    assertResult(-0x800)(BlockRelWorld(532,  0x800, 17, 3, 7, 5, size).Y)
    assertResult(    -1)(BlockRelWorld(532,  0xfff, 17, 3, 7, 5, size).Y)
    assertResult(     0)(BlockRelWorld(532, 0x1000, 17, 3, 7, 5, size).Y)
  }

  test("X is correct in entire range") {
    assertResult(     -14)(BlockRelWorld(     -14, -14, 17, 3, 7, 5, size).X)
    assertResult( 0x7ffff)(BlockRelWorld( 0x7ffff, -14, 17, 3, 7, 5, size).X)
    assertResult(-0x80000)(BlockRelWorld( 0x80000, -14, 17, 3, 7, 5, size).X)
    assertResult(      -1)(BlockRelWorld( 0xfffff, -14, 17, 3, 7, 5, size).X)
    assertResult(       0)(BlockRelWorld(0x100000, -14, 17, 3, 7, 5, size).X)
  }

  test("Z is correct in entire range") {
    assertResult( 1)(BlockRelWorld(532, -14,  1, 3, 7, 5, size).Z)
    assertResult(15)(BlockRelWorld(532, -14, -1, 3, 7, 5, size).Z)
    assertResult( 0)(BlockRelWorld(532, -14, 16, 3, 7, 5, size).Z)
    assertResult( 0)(BlockRelWorld(532, -14, 16 * 25235, 3, 7, 5, size).Z)
  }

  test("xyz is correct in normal range") {
    val c = BlockRelWorld(532, -14, 17, 3, 7, 5, size)
    assertResult(532 * 16 + 3)(c.x)
    assertResult(-14 * 16 + 7)(c.y)
    assertResult(  1 * 16 + 5)(c.z)
  }

  test("convenient constructor works") {
    val c = BlockRelWorld(532 * 16 + 3, -14 * 16 + 7, -15 * 16 + 5, size)
    assertResult(532 * 16 + 3)(c.x)
    assertResult(-14 * 16 + 7)(c.y)
    assertResult(  1 * 16 + 5)(c.z)
  }

  test("offset works") {
    val c = BlockRelWorld(532, -14, 17, 3, 7, 5, size).offset(-4, 71, -12345)
    assertResult(532 * 16 -  1)(c.x)
    assertResult(-14 * 16 + 78)(c.y)
    assertResult( 13 * 16 + 12)(c.z)
  }

  test("value is in XXXXXZZZZZYYYxyz-format and correct") {
    assertResult(532L << 44 | (17L & 15) << 24 | (-14 & 0xfff) << 12 | 3 << 8 | 7 << 4 | 5)(BlockRelWorld(532, -14, 17, 3, 7, 5, size).value)
    assertResult((-532L & 0xfffff) << 44 | (17L & 15) << 24 | (-14 & 0xfff) << 12 | 3 << 8 | 7 << 4 | 5)(BlockRelWorld(-532, -14, 17, 3, 7, 5, size).value)
    assertResult(532L << 44 | (-17L & 15) << 24 | (-14 & 0xfff) << 12 | 3 << 8 | 7 << 4 | 5)(BlockRelWorld(532, -14, -17, 3, 7, 5, size).value)
  }
  
  private def size = new CylinderSize(4)
}
