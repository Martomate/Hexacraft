package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BlockRelWorldTest extends AnyFlatSpec with Matchers {
  private val cylSize = CylinderSize(4)
  import cylSize.impl

  "chunk xyz" should "be correct in normal range" in {
    val c = BlockRelWorld(532, -14, 17, 3, 7, 5)
    c.cx shouldBe 3
    c.cy shouldBe 7
    c.cz shouldBe 5
  }

  it should "be correct outside normal range" in {
    val c = BlockRelWorld(532, -14, 17, 3 + 5 * 16, 7 - 3 * 16, 5 + 200 * 16)
    c.cx shouldBe 3
    c.cy shouldBe 7
    c.cz shouldBe 5
  }

  "Y" should "be correct in entire range" in {
    BlockRelWorld(532, -14, 17, 3, 7, 5).Y shouldBe -14
    BlockRelWorld(532, 0x7ff, 17, 3, 7, 5).Y shouldBe 0x7ff
    BlockRelWorld(532, 0x800, 17, 3, 7, 5).Y shouldBe -0x800
    BlockRelWorld(532, 0xfff, 17, 3, 7, 5).Y shouldBe -1
    BlockRelWorld(532, 0x1000, 17, 3, 7, 5).Y shouldBe 0
  }

  "X" should "be correct in entire range" in {
    BlockRelWorld(-14, -14, 17, 3, 7, 5).X shouldBe -14
    BlockRelWorld(0x7ffff, -14, 17, 3, 7, 5).X shouldBe 0x7ffff
    BlockRelWorld(0x80000, -14, 17, 3, 7, 5).X shouldBe -0x80000
    BlockRelWorld(0xfffff, -14, 17, 3, 7, 5).X shouldBe -1
    BlockRelWorld(0x100000, -14, 17, 3, 7, 5).X shouldBe 0
  }

  "Z" should "be correct in entire range" in {
    BlockRelWorld(532, -14, 1, 3, 7, 5).Z shouldBe 1
    BlockRelWorld(532, -14, -1, 3, 7, 5).Z shouldBe 15
    BlockRelWorld(532, -14, 16, 3, 7, 5).Z shouldBe 0
    BlockRelWorld(532, -14, 16 * 25235, 3, 7, 5).Z shouldBe 0
  }

  "xyz" should "be correct in normal range" in {
    val c = BlockRelWorld(532, -14, 17, 3, 7, 5)
    c.x shouldBe 532 * 16 + 3
    c.y shouldBe -14 * 16 + 7
    c.z shouldBe 1 * 16 + 5
  }

  "convenient constructor" should "give correct result" in {
    val c = BlockRelWorld(532 * 16 + 3, -14 * 16 + 7, -15 * 16 + 5)
    c.x shouldBe 532 * 16 + 3
    c.y shouldBe -14 * 16 + 7
    c.z shouldBe 1 * 16 + 5
  }

  "offset" should "give correct result" in {
    val c = BlockRelWorld(532, -14, 17, 3, 7, 5).offset(-4, 71, -12345)
    c.x shouldBe 532 * 16 - 1
    c.y shouldBe -14 * 16 + 78
    c.z shouldBe 13 * 16 + 12
  }

  "value" should "be in XXXXXZZZZZYYYxyz-format and correct" in {
    BlockRelWorld(532, -14, 17, 3, 7, 5).value shouldBe
      532L << 44 | (17L & 15) << 24 | (-14 & 0xfff) << 12 | 3 << 8 | 7 << 4 | 5
    BlockRelWorld(-532, -14, 17, 3, 7, 5).value shouldBe
      (-532L & 0xfffff) << 44 | (17L & 15) << 24 | (-14 & 0xfff) << 12 | 3 << 8 | 7 << 4 | 5
    BlockRelWorld(532, -14, -17, 3, 7, 5).value shouldBe
      532L << 44 | (-17L & 15) << 24 | (-14 & 0xfff) << 12 | 3 << 8 | 7 << 4 | 5
  }
}
