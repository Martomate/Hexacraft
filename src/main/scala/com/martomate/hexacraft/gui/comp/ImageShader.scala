package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.renderer.{Shader, ShaderConfig}

import org.joml.{Matrix4f, Vector2f}

class ImageShader {
  private val config = ShaderConfig("image", "image").withAttribs("position")
  private val shader = Shader.register(config)

  def setTransformationMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("transformationMatrix", matrix)

  def setImageSize(width: Int, height: Int): Unit =
    shader.setUniform2f("imageSize", width.toFloat, height.toFloat)

  def enable(): Unit = shader.enable()
}
