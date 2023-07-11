package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.renderer.{Shader, Shaders}

import org.joml.{Matrix4f, Vector2f}

class ImageShader {
  private val shader: Shader = Shader.get(Shaders.ShaderNames.Image).get

  def setTransformationMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("transformationMatrix", matrix)

  def setImageSize(width: Int, height: Int): Unit =
    shader.setUniform2f("imageSize", width.toFloat, height.toFloat)

  def enable(): Unit = shader.enable()
}
