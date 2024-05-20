package hexacraft.shaders

import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.renderer.{Shader, ShaderConfig}

import org.joml.{Matrix4f, Vector4f}

class FancyRectShader {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(Vertex, "fancy_rect/vert.glsl")
      .withStage(Fragment, "fancy_rect/frag.glsl")
      .withInputs("position")
  )

  def setTransformationMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("transformationMatrix", matrix)
  }

  def setWindowAspectRatio(aspectRatio: Float): Unit = {
    shader.setUniform1f("windowAspectRatio", aspectRatio)
  }

  def setColor(color: Vector4f): Unit = {
    shader.setUniform4f("col", color)
  }

  def setInverted(inverted: Boolean): Unit = {
    shader.setUniform1i("inverted", if inverted then 1 else 0)
  }

  def enable(): Unit = {
    shader.activate()
  }
}
