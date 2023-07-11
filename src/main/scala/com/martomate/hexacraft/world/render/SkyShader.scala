package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.{Shader, ShaderConfig}

import org.joml.{Matrix4f, Vector3f}

class SkyShader {
  private val config = ShaderConfig("sky", "sky").withAttribs("position")
  private val shader = Shader.register(config)

  def setInverseProjectionMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("invProjMatr", matrix)

  def setInverseViewMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("invViewMatr", matrix)

  def setSunPosition(sun: Vector3f): Unit =
    shader.setUniform3f("sun", sun.x, sun.y, sun.z)

  def enable(): Unit = shader.enable()
}
