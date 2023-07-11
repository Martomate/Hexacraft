package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.renderer.{Shader, Shaders}

import org.joml.{Matrix4f, Vector4f}

class ColorShader {
  private val shader: Shader = Shader.get(Shaders.ShaderNames.Color).get

  def setTransformationMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("transformationMatrix", matrix)

  def setColor(color: Vector4f): Unit =
    shader.setUniform4f("col", color)

  def enable(): Unit = shader.enable()
}
