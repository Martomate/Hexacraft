package com.martomate.hexacraft.game

import com.martomate.hexacraft.renderer.{Shader, Shaders}

class CrosshairShader {
  private val shader: Shader = Shader.get(Shaders.ShaderNames.Crosshair).get

  def enable(): Unit = shader.enable()
}
