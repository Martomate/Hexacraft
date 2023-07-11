package com.martomate.hexacraft.font

import com.martomate.hexacraft.renderer.{Shader, Shaders}

import org.joml.{Vector2f, Vector3f}

class FontShader {
  private val shader = Shader.get(Shaders.ShaderNames.Font).get

  def setColor(color: Vector3f): Unit =
    shader.setUniform3f("color", color)

  def setTranslation(x: Float, y: Float): Unit =
    shader.setUniform2f("translation", x, y)

  def enable(): Unit = shader.enable()
}
