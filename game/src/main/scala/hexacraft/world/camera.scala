package hexacraft.world

import hexacraft.math.MathUtils
import hexacraft.world.coord.{BlockRelWorld, CoordUtils, CylCoords}

import org.joml.{Matrix4f, Vector3d, Vector3f, Vector3fc, Vector4d}

class Camera(val proj: CameraProjection)(using worldSize: CylinderSize) {
  val view = new CameraView
  def position: Vector3d = view.position
  def rotation: Vector3f = view.rotation

  var blockCoords: BlockRelWorld = BlockRelWorld(0)

  updateViewMatrix(view.position)
  updateProjMatrix()

  def setPosition(vec: Vector3d): Unit = {
    setPosition(vec.x, vec.y, vec.z)
  }

  def setPosition(x: Double, y: Double, z: Double): Unit = {
    position.x = x
    position.y = y
    position.z = MathUtils.fitZ(z, worldSize.circumference)
  }

  def setPositionAndRotation(position: Vector3d, rotation: Vector3d): Unit = {
    setPosition(position)
    setRotation(rotation.x.toFloat, rotation.y.toFloat, rotation.z.toFloat)
  }

  def setRotation(vec: Vector3f): Unit = {
    setRotation(vec.x, vec.y, vec.z)
  }

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

  def updateViewMatrix(origin: Vector3d): Unit = {
    view.updateViewMatrix(origin)
  }

  def updateProjMatrix(): Unit = {
    proj.updateProjMatrix()
  }

  def updateCoords(): Unit = {
    val temp = CoordUtils.getEnclosingBlock(CylCoords(position).toBlockCoords)
    blockCoords = temp._1
  }
}

class CameraProjection(var fov: Float, var aspect: Float, val near: Float, val far: Float) {
  val matrix = new Matrix4f
  val invMatrix = new Matrix4f

  def updateProjMatrix(): Unit = {
    matrix.identity()
    matrix.perspective(fov, aspect, near, far)
    matrix.invert(invMatrix)
  }
}

object CameraView {
  private val unitX: Vector3fc = new Vector3f(1, 0, 0)
  private val unitY: Vector3fc = new Vector3f(0, 1, 0)
  private val unitZ: Vector3fc = new Vector3f(0, 0, 1)
}

class CameraView {
  val position = new Vector3d
  val rotation = new Vector3f
  val matrix = new Matrix4f
  val invMatrix = new Matrix4f

  def updateViewMatrix(origin: Vector3d): Unit = {
    val pos = position.sub(origin, new Vector3d)
    matrix.identity()
    matrix.translation(pos.x.toFloat, pos.y.toFloat, pos.z.toFloat)
    matrix.rotate(rotation.z, CameraView.unitZ)
    matrix.rotate(rotation.x, CameraView.unitX)
    matrix.rotate(rotation.y, CameraView.unitY)
    matrix.invert(invMatrix)
  }

  def forward: Vector3d = new Vector4d(0, 0, -1, 0).mul(this.invMatrix).xyz(new Vector3d)
}
