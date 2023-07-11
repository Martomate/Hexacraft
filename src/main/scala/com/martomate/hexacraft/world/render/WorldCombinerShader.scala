package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.infra.gpu.OpenGL
import com.martomate.hexacraft.renderer.{Shader, ShaderConfig}

class WorldCombinerShader {
  private val config = ShaderConfig("world_combiner", "world_combiner").withAttribs("position")
  private val shader = Shader.register(config)

  shader.setUniform1i("worldColorTexture", 0)
  shader.setUniform1i("worldDepthTexture", 1)

  def colorTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(0)
  def depthTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(1)

  def setClipPlanes(nearPlane: Float, farPlane: Float): Unit =
    shader.setUniform1f("nearPlane", nearPlane)
    shader.setUniform1f("farPlane", farPlane)

  def enable(): Unit = shader.enable()
}
