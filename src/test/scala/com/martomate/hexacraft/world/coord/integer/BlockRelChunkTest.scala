package com.martomate.hexacraft.world.coord.integer

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BlockRelChunkTest extends AnyFlatSpec with Matchers {
  "xyz" should "be correct in normal range" in {
    val c = BlockRelChunk(3, 7, 5)
    c.cx shouldBe 3
    c.cy shouldBe 7
    c.cz shouldBe 5
  }

  it should "be correct outside normal range" in {
    val c = BlockRelChunk(3 + 5*16, 7 - 3*16, 5 + 200 * 16)
    c.cx shouldBe 3
    c.cy shouldBe 7
    c.cz shouldBe 5
  }

  "value" should "be in xyz-format and correct" in {
    val c = BlockRelChunk(3, 7, 5)
    c.value shouldBe (3 << 8 | 7 << 4 | 5)
  }

  "offset" should "be correct" in {
    val c = BlockRelChunk(3, 7, 5).offset(-4, -1, 14)
    c.cx shouldBe 15
    c.cy shouldBe 6
    c.cz shouldBe 3
  }
}
