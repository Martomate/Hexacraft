package hexacraft.gui.comp

import hexacraft.renderer.{Shader, ShaderConfig}

import org.joml.{Matrix4f, Vector4f}

class FancyRectShader {
  private val config = ShaderConfig("fancy_rect").withAttribs("position")
  private val shader = Shader.from(config)

  def setTransformationMatrix(matrix: Matrix4f): Unit =
    shader.setUniformMat4("transformationMatrix", matrix)

  def setWindowAspectRatio(aspectRatio: Float): Unit =
    shader.setUniform1f("windowAspectRatio", aspectRatio)

  def setColor(color: Vector4f): Unit =
    shader.setUniform4f("col", color)

  def setInverted(inverted: Boolean): Unit =
    shader.setUniform1i("inverted", if inverted then 1 else 0)

  def enable(): Unit = shader.activate()
}
