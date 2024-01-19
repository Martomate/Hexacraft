package hexacraft.text

import hexacraft.renderer.{Shader, ShaderConfig}

import org.joml.Vector3f

class FontShader {
  private val config = ShaderConfig("font").withInputs("position", "textureCoords")
  private val shader = Shader.from(config)

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
