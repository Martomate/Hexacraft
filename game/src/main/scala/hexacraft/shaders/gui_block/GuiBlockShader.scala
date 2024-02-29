package hexacraft.shaders.gui_block

import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.renderer.{Shader, ShaderConfig}

import org.joml.Matrix4f

class GuiBlockShader(isSide: Boolean) {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(Vertex, "gui_block/vert.glsl")
      .withStage(Fragment, "gui_block/frag.glsl")
      .withInputs(
        "position",
        "texCoords",
        "normal",
        "vertexIndex",
        "faceIndex",
        "blockPos",
        "blockTex",
        "blockHeight",
        "brightness"
      )
      .withDefines("isSide" -> (if isSide then "1" else "0"))
  )

  def setWindowAspectRatio(aspectRatio: Float): Unit = {
    shader.setUniform1f("windowAspectRatio", aspectRatio)
  }

  def setProjectionMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("projMatrix", matrix)
  }

  def setViewMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("viewMatrix", matrix)
  }

  def setSide(side: Int): Unit = {
    shader.setUniform1i("side", side)
  }

  def enable(): Unit = {
    shader.activate()
  }
}
