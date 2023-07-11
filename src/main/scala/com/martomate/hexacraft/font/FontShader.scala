package com.martomate.hexacraft.font

import com.martomate.hexacraft.renderer.{Shader, ShaderConfig}

import org.joml.{Vector2f, Vector3f}

class FontShader {
  private val config = ShaderConfig("font", "font").withAttribs("position", "textureCoords")
  private val shader = Shader.register(config)

  def setColor(color: Vector3f): Unit =
    shader.setUniform3f("color", color)

  def setTranslation(x: Float, y: Float): Unit =
    shader.setUniform2f("translation", x, y)

  def enable(): Unit = shader.enable()
}
