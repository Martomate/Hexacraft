package hexacraft.world.camera

import hexacraft.math.MathUtils
import hexacraft.world.CylinderSize
import hexacraft.world.coord.CoordUtils
import hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import hexacraft.world.coord.integer.BlockRelWorld

import org.joml.{Vector3d, Vector3f}

class Camera(val proj: CameraProjection)(using worldSize: CylinderSize) {
  val view = new CameraView
  def position: Vector3d = view.position
  def rotation: Vector3f = view.rotation

  var blockCoords: BlockRelWorld = _
  var placeInBlock: BlockCoords.Offset = _

  updateViewMatrix()
  updateProjMatrix()

  def setPosition(vec: Vector3d): Unit = setPosition(vec.x, vec.y, vec.z)

  def setPosition(x: Double, y: Double, z: Double): Unit = {
    position.x = x
    position.y = y
    position.z = MathUtils.fitZ(z, worldSize.circumference)
  }

  def setPositionAndRotation(position: Vector3d, rotation: Vector3d): Unit = {
    setPosition(position)
    setRotation(rotation.x.toFloat, rotation.y.toFloat, rotation.z.toFloat)
  }

  def setRotation(vec: Vector3f): Unit = setRotation(vec.x, vec.y, vec.z)

  def setRotation(x: Float, y: Float, z: Float): Unit = {
    rotation.x = x
    rotation.y = y
    rotation.z = z
  }

  def rotate(x: Float, y: Float, z: Float): Unit = {
    rotation.x += x
    rotation.y += y
    rotation.z += z
  }

  def updateViewMatrix(): Unit = {
    view.updateViewMatrix()
  }

  def updateProjMatrix(): Unit = {
    proj.updateProjMatrix()
  }

  def updateCoords(): Unit = {
    val temp = CoordUtils.getEnclosingBlock(CylCoords(position).toBlockCoords)
    blockCoords = temp._1
    placeInBlock = temp._2
  }
}
