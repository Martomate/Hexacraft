package hexacraft.world.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{Shader, ShaderConfig, VAO}

class WorldCombinerShader {
  private val config = ShaderConfig("world_combiner").withAttribs("position")
  private val shader = Shader.from(config)

  shader.setUniform1i("worldColorTexture", 0)
  shader.setUniform1i("worldDepthTexture", 1)

  def colorTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(0)
  def depthTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(1)

  def setClipPlanes(nearPlane: Float, farPlane: Float): Unit =
    shader.setUniform1f("nearPlane", nearPlane)
    shader.setUniform1f("farPlane", farPlane)

  def enable(): Unit = shader.activate()

  def free(): Unit = shader.free()
}

object WorldCombinerVao {
  def create: VAO =
    VAO
      .builder()
      .addVertexVbo(4)(
        _.floats(0, 2),
        _.fillFloats(0, Seq(-1, -1, 1, -1, -1, 1, 1, 1))
      )
      .finish(4)
}
