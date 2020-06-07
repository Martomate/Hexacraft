package com.martomate.hexacraft.util

import org.scalatest.funsuite.AnyFunSuite

class CylinderSizeTest extends AnyFunSuite {
  test("all aspects of the cylinder size should be determined by worldSize") {
    for (s <- 1 to 20) {
      val size = new CylinderSize(s)
      assertResult(s)(size.worldSize)
      assertResult(1 << size.worldSize)(size.ringSize)
      assertResult(size.ringSize - 1)(size.ringSizeMask)
      assertResult(size.ringSize * 16)(size.totalSize)
      assertResult(size.totalSize - 1)(size.totalSizeMask)
      assertResult((2 * math.Pi) / size.totalSize)(size.hexAngle)
      assertResult(CylinderSize.y60 / size.hexAngle)(size.radius)
      assertResult(size.totalSize * CylinderSize.y60)(size.circumference)
    }
  }
}
