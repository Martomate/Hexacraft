package hexacraft.shaders.world_combiner

import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.renderer.{GpuState, Renderer, Shader, ShaderConfig, VAO}

class WorldCombinerShader {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(Vertex, "world_combiner/vert.glsl")
      .withStage(Fragment, "world_combiner/frag.glsl")
      .withInputs("position")
  )

  shader.setUniform1i("worldColorTexture", 0)
  shader.setUniform1i("worldDepthTexture", 1)

  def colorTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(0)
  def depthTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(1)

  def setClipPlanes(nearPlane: Float, farPlane: Float): Unit = {
    shader.setUniform1f("nearPlane", nearPlane)
    shader.setUniform1f("farPlane", farPlane)
  }

  def enable(): Unit = {
    shader.activate()
  }

  def free(): Unit = {
    shader.free()
  }
}

object WorldCombinerShader {
  def createVao(): VAO = {
    VAO
      .builder()
      .addVertexVbo(4)(
        _.floats(0, 2),
        _.fillFloats(0, Seq(-1, -1, 1, -1, -1, 1, 1, 1))
      )
      .finish(4)
  }

  def createRenderer(): Renderer = new Renderer(OpenGL.PrimitiveMode.TriangleStrip, GpuState.build(_.depthTest(false)))
}
