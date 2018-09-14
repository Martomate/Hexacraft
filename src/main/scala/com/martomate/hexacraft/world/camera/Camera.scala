package com.martomate.hexacraft.world.camera

import com.martomate.hexacraft.resource.Shader
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.CoordUtils.toBlockCoords
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.player.Player
import org.joml.{Vector3d, Vector3f}

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
    val temp = toBlockCoords()
    blockCoords = temp._1
    placeInBlock = temp._2
  }

  private def toBlockCoords(adjustY: Boolean = true): (BlockRelWorld, BlockCoords) = {
    val camX = position.x
    val y = position.y * 2
    val mult = if (adjustY) Math.exp(y - position.y) else 1
    val x = ((position.x - camX) / mult + camX) / 0.75
    val z = position.z / CylinderSize.y60 - x / 2

    CoordUtils.toBlockCoords(BlockCoords(x, y, z, worldSize))
  }
}
