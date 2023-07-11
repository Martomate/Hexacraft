package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.infra.gpu.OpenGL
import com.martomate.hexacraft.renderer.{Shader, Shaders}

class WorldCombinerShader {
  private val shader = Shader.get(Shaders.ShaderNames.WorldCombiner).get

  shader.setUniform1i("worldColorTexture", 0)
  shader.setUniform1i("worldDepthTexture", 1)

  def colorTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(0)
  def depthTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(1)

  def setClipPlanes(nearPlane: Float, farPlane: Float): Unit =
    shader.setUniform1f("nearPlane", nearPlane)
    shader.setUniform1f("farPlane", farPlane)

  def enable(): Unit = shader.enable()
}
