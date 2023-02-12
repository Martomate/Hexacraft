package com.martomate.hexacraft.world.coord.integer

import munit.FunSuite

class BlockRelChunkTest extends FunSuite {
  test("xyz should be correct in normal range") {
    val c = BlockRelChunk(3, 7, 5)
    assertEquals(c.cx, 3.toByte)
    assertEquals(c.cy, 7.toByte)
    assertEquals(c.cz, 5.toByte)
  }

  test("xyz should be correct outside normal range") {
    val c = BlockRelChunk(3 + 5 * 16, 7 - 3 * 16, 5 + 200 * 16)
    assertEquals(c.cx, 3.toByte)
    assertEquals(c.cy, 7.toByte)
    assertEquals(c.cz, 5.toByte)
  }

  test("value should be in xyz-format and correct") {
    val c = BlockRelChunk(3, 7, 5)
    assertEquals(c.value, (3 << 8 | 7 << 4 | 5))
  }

  test("offset should be correct") {
    val c = BlockRelChunk(3, 7, 5).offset(-4, -1, 14)
    assertEquals(c.cx, 15.toByte)
    assertEquals(c.cy, 6.toByte)
    assertEquals(c.cz, 3.toByte)
  }
}
