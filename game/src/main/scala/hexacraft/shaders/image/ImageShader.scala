package hexacraft.shaders.image

import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.renderer.{Shader, ShaderConfig}

import org.joml.Matrix4f

class ImageShader {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(Vertex, "image/vert.glsl")
      .withStage(Fragment, "image/frag.glsl")
      .withInputs("position")
  )

  def setTransformationMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("transformationMatrix", matrix)
  }

  def setWindowAspectRatio(aspectRatio: Float): Unit = {
    shader.setUniform1f("windowAspectRatio", aspectRatio)
  }

  def enable(): Unit = {
    shader.activate()
  }
}
