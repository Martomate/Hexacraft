package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.renderer.{Shader, ShaderConfig}
import org.joml.{Matrix4f, Vector2f}

class ImageShader {
  private val config = ShaderConfig("image").withAttribs("position")
  private val shader = Shader.from(config)

  def setTransformationMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("transformationMatrix", matrix)

  def setWindowAspectRatio(aspectRatio: Float): Unit =
    shader.setUniform1f("windowAspectRatio", aspectRatio)

  def enable(): Unit = shader.activate()
}
