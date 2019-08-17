package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize
import org.scalatest.{FlatSpec, Matchers}

class ColumnRelWorldTest extends FlatSpec with Matchers {
  private val cylSize = new CylinderSize(4)
  import cylSize.impl
  
  "X" should "be correct in entire range" in {
    ColumnRelWorld(     -14, 17).X shouldBe -14
    ColumnRelWorld( 0x7ffff, 17).X shouldBe 0x7ffff
    ColumnRelWorld( 0x80000, 17).X shouldBe -0x80000
    ColumnRelWorld( 0xfffff, 17).X shouldBe -1
    ColumnRelWorld(0x100000, 17).X shouldBe 0
  }

  "Z" should "be correct in entire range" in {
    ColumnRelWorld(532,  1).Z shouldBe 1
    ColumnRelWorld(532, -1).Z shouldBe 15
    ColumnRelWorld(532, 16).Z shouldBe 0
    ColumnRelWorld(532, 16 * 25235).Z shouldBe 0
  }

  "value" should "be in XXXXXZZZZZYYY-format and correct" in {
    ColumnRelWorld( 532,  17).value shouldBe 532L << 20 | (17L & 15)
    ColumnRelWorld(-532,  17).value shouldBe (-532L & 0xfffff) << 20 | (17L & 15)
    ColumnRelWorld( 532, -17).value shouldBe 532L << 20 | (-17L & 15)
  }
}
