package hexacraft.shaders.crosshair

import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.renderer.{Shader, ShaderConfig}

class CrosshairShader {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(Vertex, "crosshair/vert.glsl")
      .withStage(Fragment, "crosshair/frag.glsl")
      .withInputs("position")
  )

  def setWindowAspectRatio(aspectRatio: Float): Unit = {
    shader.setUniform1f("windowAspectRatio", aspectRatio)
  }

  def enable(): Unit = {
    shader.activate()
  }

  def free(): Unit = {
    shader.free()
  }
}
