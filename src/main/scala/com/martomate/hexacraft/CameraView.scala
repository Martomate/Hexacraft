package com.martomate.hexacraft

import org.joml.{Matrix4f, Vector3d, Vector3f}

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
