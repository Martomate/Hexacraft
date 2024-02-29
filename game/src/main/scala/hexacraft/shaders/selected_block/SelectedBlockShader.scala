package hexacraft.shaders.selected_block

import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.renderer.{Shader, ShaderConfig}

import org.joml.{Matrix4f, Vector3d}

class SelectedBlockShader {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(Vertex, "selected_block/vert.glsl")
      .withStage(Fragment, "selected_block/frag.glsl")
      .withInputs("position", "blockPos", "color", "blockHeight")
  )

  def setTotalSize(totalSize: Int): Unit = {
    shader.setUniform1i("totalSize", totalSize)
  }

  def setCameraPosition(cam: Vector3d): Unit = {
    shader.setUniform3f("cam", cam.x.toFloat, cam.y.toFloat, cam.z.toFloat)
  }

  def setProjectionMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("projMatrix", matrix)
  }

  def setViewMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("viewMatrix", matrix)
  }

  def enable(): Unit = {
    shader.activate()
  }

  def free(): Unit = {
    shader.free()
  }
}
