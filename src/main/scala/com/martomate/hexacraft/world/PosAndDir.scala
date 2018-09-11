package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.camera.CameraView
import com.martomate.hexacraft.world.coord.fp.CylCoords
import org.joml.{Vector3d, Vector4d}

class PosAndDir(cylinderSize: CylinderSize) {
  private var _pos: CylCoords = CylCoords(0, 0, 0, cylinderSize)
  def pos: CylCoords = _pos
  val dir: Vector3d = new Vector3d()

  def setPosAndDirFrom(camera: CameraView): Vector3d = {
    _pos = CylCoords(camera.position.x, camera.position.y, camera.position.z, cylinderSize)
    val vec4 = new Vector4d(0, 0, -1, 0).mul(camera.invMatrix)
    dir.set(vec4.x, vec4.y, vec4.z) // new Vector3d(0, 0, -1).rotateX(-player.rotation.x).rotateY(-player.rotation.y))
  }
}
