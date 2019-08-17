package com.martomate.hexacraft.world.camera

import org.joml.Matrix4f

class CameraProjection(var fov: Float, var aspect: Float, val near: Float, val far: Float) {
  val matrix = new Matrix4f
  val invMatrix = new Matrix4f

  def updateProjMatrix(): Unit = {
    matrix.identity()
    matrix.perspective(fov, aspect, near, far)
    matrix.invert(invMatrix)
  }
}
