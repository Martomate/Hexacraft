package hexacraft.shaders.font

import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.renderer.{Shader, ShaderConfig}

import org.joml.Vector3f

class FontShader {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(Vertex, "font/vert.glsl")
      .withStage(Fragment, "font/frag.glsl")
      .withInputs("position", "textureCoords")
  )

  def setWindowAspectRatio(aspectRatio: Float): Unit = {
    shader.setUniform1f("windowAspectRatio", aspectRatio)
  }

  def setColor(color: Vector3f): Unit = {
    shader.setUniform3f("color", color)
  }

  def setTranslation(x: Float, y: Float): Unit = {
    shader.setUniform2f("translation", x, y)
  }

  def enable(): Unit = {
    shader.activate()
  }
}
