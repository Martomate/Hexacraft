package com.martomate.hexacraft.world

import com.martomate.hexacraft.world.camera.Camera

trait DebugInfoProvider {
  def camera: Camera

  def viewDistance: Double
}
