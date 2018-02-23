package hexagon

import org.joml.{Matrix4f, Vector3d, Vector3f, Vector3fc}
import hexagon.resource.Shader
import hexagon.world.coord.BlockCoords
import hexagon.world.coord.BlockRelWorld
import hexagon.world.coord.CoordUtils
import hexagon.world.storage.World
import hexagon.world.Player

object Camera {
  val unitX: Vector3fc = new Vector3f(1, 0, 0).toImmutable
  val unitY: Vector3fc = new Vector3f(0, 1, 0).toImmutable
  val unitZ: Vector3fc = new Vector3f(0, 0, 1).toImmutable
}

class Camera(var fov: Float, var aspect: Float, val near: Float, val far: Float, val world: World) {
  val position = new Vector3d
  val rotation = new Vector3f
  var blockCoords: BlockRelWorld = _
  var placeInBlock: BlockCoords = _
  val projMatrix = new Matrix4f
  val viewMatrix = new Matrix4f
  val invProjMatr = new Matrix4f
  val invViewMatr = new Matrix4f

  val updateUniforms: Shader => Unit = s => {
    s.setUniformMat4("viewMatrix", viewMatrix)
    s.setUniform3f("cam", position.x.toFloat, position.y.toFloat, position.z.toFloat)
  }
  val setProjMatrix: Shader => Unit = _.setUniformMat4("projMatrix", projMatrix)

  updateViewMatrix()
  updateProjMatrix()

  def setPosition(vec: Vector3d): Unit = setPosition(vec.x, vec.y, vec.z)
  
  def setPosition(x: Double, y: Double, z: Double): Unit = {
    position.x = x
    position.y = y
    position.z = z
    position.z %= world.circumference
    if (position.z < 0) position.z += world.circumference
  }
  
  def setPositionAndRotation(player: Player): Unit = {
    setPosition(player.position)
    setRotation(player.rotation.x.toFloat, player.rotation.y.toFloat, player.rotation.z.toFloat)
  }

  def move(x: Double, y: Double, z: Double): Unit = {
    position.x += x
    position.y += y
    position.z += z
    position.z %= world.circumference
    if (position.z < 0) position.z += world.circumference
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
    viewMatrix.identity()
    viewMatrix.rotate(rotation.z, Camera.unitZ)
    viewMatrix.rotate(rotation.x, Camera.unitX)
    viewMatrix.rotate(rotation.y, Camera.unitY)
    viewMatrix.invert(invViewMatr)
  }

  def updateProjMatrix(): Unit = {
    projMatrix.identity()
    projMatrix.perspective(fov, aspect, near, far)
    projMatrix.invert(invProjMatr)
  }

  def updateCoords(): Unit = {
    val temp = CoordUtils.toBlockCoords(this, world, position)
    blockCoords = temp._1
    placeInBlock = temp._2
  }
}
