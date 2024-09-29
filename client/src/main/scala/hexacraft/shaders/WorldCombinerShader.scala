package hexacraft.shaders

import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.renderer.*

import org.joml.Vector3f

class WorldCombinerShader {
  private val shader = Shader.from(
    ShaderConfig()
      .withStage(Vertex, "world_combiner/vert.glsl")
      .withStage(Fragment, "world_combiner/frag.glsl")
      .withInputs("position")
  )

  shader.setUniform1i("worldPositionTexture", 0)
  shader.setUniform1i("worldNormalTexture", 1)
  shader.setUniform1i("worldColorTexture", 2)
  shader.setUniform1i("worldDepthTexture", 3)

  def positionTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(0)
  def normalTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(1)
  def colorTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(2)
  def depthTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(3)

  def setClipPlanes(nearPlane: Float, farPlane: Float): Unit = {
    shader.setUniform1f("nearPlane", nearPlane)
    shader.setUniform1f("farPlane", farPlane)
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

object WorldCombinerShader {
  def createVao(): VAO = {
    VAO.build(4)(
      _.addVertexVbo(4)(
        _.floats(0, 2),
        _.fillFloats(0, Seq(-1, -1, 1, -1, -1, 1, 1, 1))
      )
    )
  }

  def createRenderer(): Renderer =
    new Renderer(OpenGL.PrimitiveMode.TriangleStrip, GpuState.build(_.depthTest(false).blend(true)))
}
