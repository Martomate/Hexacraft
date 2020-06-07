package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ChunkRelColumnTest extends AnyFlatSpec with Matchers {
  private val cylSize = new CylinderSize(4)
  import cylSize.impl

  "Y" can "use the entire range" in {
    ChunkRelColumn.create(   -14).Y shouldBe -14
    ChunkRelColumn.create( 0x7ff).Y shouldBe 0x7ff
    ChunkRelColumn.create( 0x800).Y shouldBe -0x800
    ChunkRelColumn.create( 0xfff).Y shouldBe -1
    ChunkRelColumn.create(0x1000).Y shouldBe 0
  }

  "value" should "be in YYY-format and correct" in {
    ChunkRelColumn.create(   -14).value shouldBe -14 & 0xfff
    ChunkRelColumn.create( 0x7ff).value shouldBe 0x7ff
    ChunkRelColumn.create( 0x800).value shouldBe -0x800 & 0xfff
    ChunkRelColumn.create( 0xfff).value shouldBe -1 & 0xfff
    ChunkRelColumn.create(0x1000).value shouldBe 0
  }
}
