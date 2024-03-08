package hexacraft.shaders

import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.renderer.*
import org.joml.{Matrix4f, Vector3f}

class SkyShader {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(Vertex, "sky/vert.glsl")
      .withStage(Fragment, "sky/frag.glsl")
      .withInputs("position")
  )

  def setInverseProjectionMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("invProjMatr", matrix)
  }

  def setInverseViewMatrix(matrix: Matrix4f): Unit = {
    shader.setUniformMat4("invViewMatr", matrix)
  }

  def setSunPosition(sun: Vector3f): Unit = {
    shader.setUniform3f("sun", sun.x, sun.y, sun.z)
  }

  def enable(): Unit = {
    shader.activate()
  }

  def free(): Unit = {
    shader.free()
  }
}

object SkyShader {
  def createVao(): VAO = {
    VAO
      .builder()
      .addVertexVbo(4)(
        _.floats(0, 2),
        _.fillFloats(0, Seq(-1, -1, 1, -1, -1, 1, 1, 1))
      )
      .finish(4)
  }

  def createRenderer(): Renderer = new Renderer(OpenGL.PrimitiveMode.TriangleStrip, GpuState.build(_.depthTest(false)))
}
