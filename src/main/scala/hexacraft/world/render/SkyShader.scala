package hexacraft.world.render

import hexacraft.renderer.{Shader, ShaderConfig}

import org.joml.{Matrix4f, Vector3f}

class SkyShader {
  private val config = ShaderConfig("sky").withAttribs("position")
  private val shader = Shader.from(config)

  def setInverseProjectionMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("invProjMatr", matrix)

  def setInverseViewMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("invViewMatr", matrix)

  def setSunPosition(sun: Vector3f): Unit =
    shader.setUniform3f("sun", sun.x, sun.y, sun.z)

  def enable(): Unit = shader.activate()

  def free(): Unit = shader.free()
}
