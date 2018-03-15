package hexacraft

import org.joml.{Matrix4f, Vector3d, Vector3f, Vector3fc}
import hexacraft.resource.Shader
import hexacraft.world.coord.BlockCoords
import hexacraft.world.coord.BlockRelWorld
import hexacraft.world.coord.CoordUtils
import hexacraft.world.storage.CylinderSize
import hexacraft.world.Player

object Camera {
  val unitX: Vector3fc = new Vector3f(1, 0, 0).toImmutable
  val unitY: Vector3fc = new Vector3f(0, 1, 0).toImmutable
  val unitZ: Vector3fc = new Vector3f(0, 0, 1).toImmutable
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

class CameraView {
  val position = new Vector3d
  val rotation = new Vector3f
  val matrix = new Matrix4f
  val invMatrix = new Matrix4f

  def updateViewMatrix(): Unit = {
    matrix.identity()
    matrix.rotate(rotation.z, Camera.unitZ)
    matrix.rotate(rotation.x, Camera.unitX)
    matrix.rotate(rotation.y, Camera.unitY)
    matrix.invert(invMatrix)
  }
}

class Camera(val proj: CameraProjection, val worldSize: CylinderSize) {
  val view = new CameraView
  def position: Vector3d = view.position
  def rotation: Vector3f = view.rotation

  var blockCoords: BlockRelWorld = _
  var placeInBlock: BlockCoords = _

  updateViewMatrix()
  updateProjMatrix()

  def updateUniforms(s: Shader): Unit = {
    s.setUniformMat4("viewMatrix", view.matrix)
    s.setUniform3f("cam", position.x.toFloat, position.y.toFloat, position.z.toFloat)
  }
  def setProjMatrix(s: Shader): Unit = s.setUniformMat4("projMatrix", proj.matrix)

  def setPosition(vec: Vector3d): Unit = setPosition(vec.x, vec.y, vec.z)

  def setPosition(x: Double, y: Double, z: Double): Unit = {
    position.x = x
    position.y = y
    position.z = z
    position.z %= worldSize.circumference
    if (position.z < 0) position.z += worldSize.circumference
  }

  def setPositionAndRotation(player: Player): Unit = {
    setPosition(player.position)
    setRotation(player.rotation.x.toFloat, player.rotation.y.toFloat, player.rotation.z.toFloat)
  }

  def move(dx: Double, dy: Double, dz: Double): Unit = {
    setPosition(position.x + dx, position.y + dy, position.z + dz)
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
    val temp = CoordUtils.toBlockCoords(this, position)
    blockCoords = temp._1
    placeInBlock = temp._2
  }
}
