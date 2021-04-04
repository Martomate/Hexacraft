package com.martomate.hexacraft.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CylinderSizeTest extends AnyFlatSpec with Matchers {
  "all aspects of the cylinder size" should "be determined by worldSize" in {
    for (s <- 1 to 20) {
      val size = new CylinderSize(s)
      size.worldSize shouldBe s
      size.ringSize shouldBe (1 << size.worldSize)
      size.ringSizeMask shouldBe (size.ringSize - 1)
      size.totalSize shouldBe (size.ringSize * 16)
      size.totalSizeMask shouldBe (size.totalSize - 1)
      size.hexAngle shouldBe ((2 * math.Pi) / size.totalSize)
      size.radius shouldBe (CylinderSize.y60 / size.hexAngle)
      size.circumference shouldBe (size.totalSize * CylinderSize.y60)
    }
  }
}
