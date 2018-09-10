package com.martomate.hexacraft.scene

import com.martomate.hexacraft.Camera

trait DebugInfoProvider {
  def camera: Camera
  def viewDistance: Double
}
