package hexacraft.shaders

import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.renderer.{Shader, ShaderConfig}
import org.joml.{Matrix4f, Vector4f}

class ColorShader {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(Vertex, "color/vert.glsl")
      .withStage(Fragment, "color/frag.glsl")
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

  def enable(): Unit = {
    shader.activate()
  }
}
