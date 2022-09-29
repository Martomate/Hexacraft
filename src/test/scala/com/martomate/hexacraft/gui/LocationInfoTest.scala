package com.martomate.hexacraft.gui

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.gui.LocationInfo16x9
import org.scalatest.flatspec.AnyFlatSpec

class LocationInfoTest extends AnyFlatSpec {
  implicit val windowImplicit: GameWindow = null

  "16x9" should "have a 16x9 aspect ratio" in {
    val info = LocationInfo16x9(0, 0, 1, 1)
    assertResult(-16f / 9)(info.x)
    assertResult(-1)(info.y)
    assertResult(2 * 16f / 9)(info.w)
    assertResult(2)(info.h)
  }

  it should "have a 16x9 aspect ratio at arbitrary point" in {
    val info = LocationInfo16x9(0.3f, 0.23f, 0.78f, 0.54f)
    assertResult((0.3f * 2 - 1) * 16f / 9)(info.x)
    assertResult(0.23f * 2 - 1)(info.y)
    assertResult(0.78f * 2 * 16f / 9)(info.w)
    assertResult(0.54f * 2)(info.h)
  }
}
