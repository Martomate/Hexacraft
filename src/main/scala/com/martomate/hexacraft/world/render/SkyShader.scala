package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.{Shader, Shaders}

import org.joml.{Matrix4f, Vector3f}

class SkyShader {
  private val shader = Shader.get(Shaders.ShaderNames.Sky).get

  def setInverseProjectionMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("invProjMatr", matrix)

  def setInverseViewMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("invViewMatr", matrix)

  def setSunPosition(sun: Vector3f): Unit =
    shader.setUniform3f("sun", sun.x, sun.y, sun.z)

  def enable(): Unit = shader.enable()
}
