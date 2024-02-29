package hexacraft.shaders.selected_block

import hexacraft.renderer.{Shader, ShaderConfig}

import org.joml.{Matrix4f, Vector3d}

class SelectedBlockShader {
  private val config = ShaderConfig("selected_block/selected_block")
    .withInputs("position", "blockPos", "color", "blockHeight")

  private val shader = Shader.from(config)

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
