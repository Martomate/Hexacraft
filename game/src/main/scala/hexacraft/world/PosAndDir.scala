package hexacraft.world

import hexacraft.world.{CameraView, CylinderSize}
import hexacraft.world.coord.CylCoords

import org.joml.{Vector3d, Vector4d}

case class PosAndDir(var pos: CylCoords, dir: Vector3d = new Vector3d()) {
  def setPosAndDirFrom(camera: CameraView)(using CylinderSize): Vector3d = {
    pos = CylCoords(camera.position)
    val vec4 = new Vector4d(0, 0, -1, 0).mul(camera.invMatrix)
    dir.set(vec4.x, vec4.y, vec4.z)
  }
}

object PosAndDir {
  def fromCameraView(camera: CameraView)(using CylinderSize): PosAndDir = {
    val pos = CylCoords(camera.position)
    val vec4 = new Vector4d(0, 0, -1, 0).mul(camera.invMatrix)
    val dir: Vector3d = new Vector3d()
    dir.set(vec4.x, vec4.y, vec4.z)
    PosAndDir(pos, dir)
  }
}
