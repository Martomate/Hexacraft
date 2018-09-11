package com.martomate.hexacraft.world.coord

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.integer.BlockRelChunk
import org.scalatest.FunSuite

class BlockRelChunkTest extends FunSuite {
  test("xyz is correct in normal range") {
    val c = BlockRelChunk(3, 7, 5, size)
    assertResult(3)(c.cx)
    assertResult(7)(c.cy)
    assertResult(5)(c.cz)
  }

  test("xyz is correct outside normal range") {
    val c = BlockRelChunk(3 + 5*16, 7 - 3*16, 5 + 200 * 16, size)
    assertResult(3)(c.cx)
    assertResult(7)(c.cy)
    assertResult(5)(c.cz)
  }

  test("value is in xyz-format and correct") {
    val c = BlockRelChunk(3, 7, 5, size)
    assertResult(3 << 8 | 7 << 4 | 5)(c.value)
  }

  test("offset is correct") {
    val c = BlockRelChunk(3, 7, 5, size).offset(-4, -1, 14)
    assertResult(15)(c.cx)
    assertResult(6)(c.cy)
    assertResult(3)(c.cz)
  }

  private def size = new CylinderSize(4)
}
