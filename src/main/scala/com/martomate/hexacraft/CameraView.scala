package com.martomate.hexacraft

import org.joml.{Matrix4f, Vector3d, Vector3f, Vector3fc}

object CameraView {
  val unitX: Vector3fc = new Vector3f(1, 0, 0)
  val unitY: Vector3fc = new Vector3f(0, 1, 0)
  val unitZ: Vector3fc = new Vector3f(0, 0, 1)
}

class CameraView {
  val position = new Vector3d
  val rotation = new Vector3f
  val matrix = new Matrix4f
  val invMatrix = new Matrix4f

  def updateViewMatrix(): Unit = {
    matrix.identity()
    matrix.rotate(rotation.z, CameraView.unitZ)
    matrix.rotate(rotation.x, CameraView.unitX)
    matrix.rotate(rotation.y, CameraView.unitY)
    matrix.invert(invMatrix)
  }
}
