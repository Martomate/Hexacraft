package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ChunkRelWorldTest extends AnyFlatSpec with Matchers {
  private val cylSize = CylinderSize(4)
  import cylSize.impl

  "Y" should "be correct in entire range" in {
    ChunkRelWorld(532,    -14, 17).Y shouldBe -14
    ChunkRelWorld(532,  0x7ff, 17).Y shouldBe 0x7ff
    ChunkRelWorld(532,  0x800, 17).Y shouldBe -0x800
    ChunkRelWorld(532,  0xfff, 17).Y shouldBe -1
    ChunkRelWorld(532, 0x1000, 17).Y shouldBe 0
  }

  "X" should "be correct in entire range" in {
    ChunkRelWorld(     -14, -14, 17).X shouldBe -14
    ChunkRelWorld( 0x7ffff, -14, 17).X shouldBe 0x7ffff
    ChunkRelWorld( 0x80000, -14, 17).X shouldBe -0x80000
    ChunkRelWorld( 0xfffff, -14, 17).X shouldBe -1
    ChunkRelWorld(0x100000, -14, 17).X shouldBe 0
  }

  "Z" should "be correct in entire range" in {
    ChunkRelWorld(532, -14,  1).Z shouldBe 1
    ChunkRelWorld(532, -14, -1).Z shouldBe 15
    ChunkRelWorld(532, -14, 16).Z shouldBe 0
    ChunkRelWorld(532, -14, 16 * 25235).Z shouldBe 0
  }

  "offset" should "give the correct result" in {
    val c = ChunkRelWorld(532, -14, 17).offset(-4, 71, -12345)
    c.X shouldBe 532 -  4
    c.Y shouldBe -14 + 71
    c.Z shouldBe 8
  }

  "value" should "be in XXXXXZZZZZYYY-format and correct" in {
    ChunkRelWorld(532, -14, 17).value shouldBe 532L << 32 | (17L & 15) << 12 | (-14 & 0xfff)
    ChunkRelWorld(-532, -14, 17).value shouldBe (-532L & 0xfffff) << 32 | (17L & 15) << 12 | (-14 & 0xfff)
    ChunkRelWorld(532, -14, -17).value shouldBe 532L << 32 | (-17L & 15) << 12 | (-14 & 0xfff)
  }

  "withBlockCoords" should "give the correct result" in {
    var c = BlockRelWorld(0, 0, 0, ChunkRelWorld(532, -14, 17))
    c.x shouldBe 532 * 16
    c.y shouldBe -14 * 16
    c.z shouldBe   1 * 16

    c = BlockRelWorld(-4, 71, -12345, ChunkRelWorld(532, -14, 17))
    c.x shouldBe 532 * 16 -  4
    c.y shouldBe -14 * 16 + 71
    c.z shouldBe (17 * 16 - 12345) & cylSize.totalSizeMask
  }
}
