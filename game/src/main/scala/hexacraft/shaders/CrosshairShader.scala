package hexacraft.shaders

import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.renderer.*

class CrosshairShader {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(Vertex, "crosshair/vert.glsl")
      .withStage(Fragment, "crosshair/frag.glsl")
      .withInputs("position")
  )

  def setWindowAspectRatio(aspectRatio: Float): Unit = {
    shader.setUniform1f("windowAspectRatio", aspectRatio)
  }

  def enable(): Unit = {
    shader.activate()
  }

  def free(): Unit = {
    shader.free()
  }
}

object CrosshairShader {
  def createVao(): VAO = {
    VAO.build(4)(
      _.addVertexVbo(4)(
        _.floats(0, 2),
        _.fillFloats(0, Seq(0, 0.03f, 0, -0.03f, -0.03f, 0, 0.03f, 0))
      )
    )
  }

  def createRenderer(): Renderer = new Renderer(OpenGL.PrimitiveMode.Lines, GpuState.build(_.depthTest(false)))
}
