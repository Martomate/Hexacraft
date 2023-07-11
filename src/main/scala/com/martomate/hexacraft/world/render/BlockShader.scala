package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.{Shader, Shaders}

import org.joml.{Matrix4f, Vector3d, Vector3f}

class BlockShader(isSide: Boolean) {
  private val shader = Shader.get(if isSide then Shaders.ShaderNames.BlockSide else Shaders.ShaderNames.Block).get

  def setTotalSize(totalSize: Int): Unit =
    shader.setUniform1i("totalSize", totalSize)

  def setSunPosition(sun: Vector3f): Unit =
    shader.setUniform3f("sun", sun.x, sun.y, sun.z)

  def setCameraPosition(cam: Vector3d): Unit =
    shader.setUniform3f("cam", cam.x.toFloat, cam.y.toFloat, cam.z.toFloat)

  def setProjectionMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("projMatrix", matrix)

  def setViewMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("viewMatrix", matrix)

  def setSide(side: Int): Unit =
    shader.setUniform1i("side", side)

  def enable(): Unit = shader.enable()
}
