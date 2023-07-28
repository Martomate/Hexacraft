package hexacraft.gui

import munit.FunSuite

class LocationInfoTest extends FunSuite {
  test("from16x9 should have a 16x9 aspect ratio") {
    val info = LocationInfo.from16x9(0, 0, 1, 1)
    assertEquals(info.x, -16f / 9)
    assertEquals(info.y, -1f)
    assertEquals(info.w, 2 * 16f / 9)
    assertEquals(info.h, 2f)
  }

  test("from16x9 should have a 16x9 aspect ratio at arbitrary point") {
    val info = LocationInfo.from16x9(0.3f, 0.23f, 0.78f, 0.54f)
    assertEquals(info.x, (0.3f * 2 - 1) * 16f / 9)
    assertEquals(info.y, 0.23f * 2 - 1)
    assertEquals(info.w, 0.78f * 2 * 16f / 9)
    assertEquals(info.h, 0.54f * 2)
  }
}
