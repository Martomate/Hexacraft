package com.martomate.hexacraft.game

import com.martomate.hexacraft.renderer.{Shader, ShaderConfig}

class CrosshairShader {
  private val config = ShaderConfig("crosshair").withAttribs("position")
  private val shader = Shader.from(config)

  def setWindowAspectRatio(aspectRatio: Float): Unit =
    shader.setUniform1f("windowAspectRatio", aspectRatio)

  def enable(): Unit = shader.enable()
}
