package hexacraft.shaders

import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.renderer.{Shader, ShaderConfig, VAO}

import org.joml.Vector4f

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

  def setColor(color: Vector4f): Unit = {
    shader.setUniform4f("color", color)
  }

  def setTranslation(x: Float, y: Float): Unit = {
    shader.setUniform2f("translation", x, y)
  }

  def enable(): Unit = {
    shader.activate()
  }
}

object FontShader {
  def createVao(vertexPositions: Seq[Float], textureCoords: Seq[Float]): VAO = {
    VAO.build(vertexPositions.length)(
      _.addVertexVbo(vertexPositions.length)(
        _.floats(0, 2),
        _.fillFloats(0, vertexPositions)
      ).addVertexVbo(textureCoords.length)(
        _.floats(1, 2),
        _.fillFloats(0, textureCoords)
      )
    )
  }
}
