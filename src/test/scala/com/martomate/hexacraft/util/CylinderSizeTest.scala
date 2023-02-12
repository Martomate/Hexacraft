package com.martomate.hexacraft.util

import munit.FunSuite

class CylinderSizeTest extends FunSuite {
  test("all aspects of the cylinder size should be determined by worldSize") {
    for (s <- 1 to 20) {
      val size = CylinderSize(s)
      assertEquals(size.worldSize, s)
      assertEquals(size.ringSize, 1 << size.worldSize)
      assertEquals(size.ringSizeMask, size.ringSize - 1)
      assertEquals(size.totalSize, size.ringSize * 16)
      assertEquals(size.totalSizeMask, size.totalSize - 1)
      assertEquals(size.hexAngle, (2 * math.Pi) / size.totalSize)
      assertEquals(size.radius, CylinderSize.y60 / size.hexAngle)
      assertEquals(size.circumference, size.totalSize * CylinderSize.y60)
    }
  }
}
