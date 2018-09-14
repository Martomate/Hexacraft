package com.martomate.hexacraft.game.debug

import com.martomate.hexacraft.world.camera.Camera

trait DebugInfoProvider {
  def camera: Camera
  def viewDistance: Double
}
