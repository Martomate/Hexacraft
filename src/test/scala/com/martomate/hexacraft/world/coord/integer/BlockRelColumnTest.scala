package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize
import org.scalatest.{FlatSpec, Matchers}

class BlockRelColumnTest extends FlatSpec with Matchers {
  private val cylSize = new CylinderSize(4)
  import cylSize.impl

  "xyz" should "be correct in normal range" in {
    val c = BlockRelColumn(-14, 3, 7, 5)
    c.cx shouldBe 3
    c.cy shouldBe 7
    c.cz shouldBe 5
  }

  it should "be correct outside normal range" in {
    val c = BlockRelColumn(-14, 3 + 5*16, 7 - 3*16, 5 + 200 * 16)
    c.cx shouldBe 3
    c.cy shouldBe 7
    c.cz shouldBe 5
  }

  "Y" should "be correct in entire range" in {
    BlockRelColumn(   -14, 3, 7, 5).Y shouldBe -14
    BlockRelColumn( 0x7ff, 3, 7, 5).Y shouldBe 0x7ff
    BlockRelColumn( 0x800, 3, 7, 5).Y shouldBe -0x800
    BlockRelColumn( 0xfff, 3, 7, 5).Y shouldBe -1
    BlockRelColumn(0x1000, 3, 7, 5).Y shouldBe 0
  }

  "value" should "be in YYYxyz-format and correct" in {
    val c = BlockRelColumn(-14, 3, 7, 5)
    c.value shouldBe (-14 & 0xfff) << 12 | 3 << 8 | 7 << 4 | 5
  }
}
