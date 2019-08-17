package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize
import org.scalatest.{FlatSpec, Matchers}

class ChunkRelColumnTest extends FlatSpec with Matchers {
  private val cylSize = new CylinderSize(4)
  import cylSize.impl

  "Y" can "use the entire range" in {
    ChunkRelColumn(   -14).Y shouldBe -14
    ChunkRelColumn( 0x7ff).Y shouldBe 0x7ff
    ChunkRelColumn( 0x800).Y shouldBe -0x800
    ChunkRelColumn( 0xfff).Y shouldBe -1
    ChunkRelColumn(0x1000).Y shouldBe 0
  }

  "value" should "be in YYY-format and correct" in {
    ChunkRelColumn(   -14).value shouldBe -14 & 0xfff
    ChunkRelColumn( 0x7ff).value shouldBe 0x7ff
    ChunkRelColumn( 0x800).value shouldBe -0x800 & 0xfff
    ChunkRelColumn( 0xfff).value shouldBe -1 & 0xfff
    ChunkRelColumn(0x1000).value shouldBe 0
  }
}
