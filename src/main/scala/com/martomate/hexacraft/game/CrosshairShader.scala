package com.martomate.hexacraft.game

import com.martomate.hexacraft.renderer.{Shader, ShaderConfig}

class CrosshairShader {
  private val config = ShaderConfig("crosshair", "crosshair").withAttribs("position")
  private val shader = Shader.register(config)

  def enable(): Unit = shader.enable()
}
