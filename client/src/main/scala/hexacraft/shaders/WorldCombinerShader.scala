package hexacraft.shaders

import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.gpu.OpenGL.ShaderType.{Fragment, Vertex}
import hexacraft.infra.gpu.OpenGL.TextureId
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

  private val positionTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(0)
  private val normalTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(1)
  private val colorTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(2)
  private val depthTextureSlot: OpenGL.TextureSlot = OpenGL.TextureSlot.ofSlot(3)

  def bindTextures(
      positionTexture: TextureId,
      normalTexture: TextureId,
      colorTexture: TextureId,
      depthTexture: TextureId
  ): Unit = {
    OpenGL.glActiveTexture(positionTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, positionTexture)
    OpenGL.glActiveTexture(normalTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, normalTexture)
    OpenGL.glActiveTexture(colorTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, colorTexture)
    OpenGL.glActiveTexture(depthTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, depthTexture)
  }

  def unbindTextures(): Unit = {
    OpenGL.glActiveTexture(depthTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, OpenGL.TextureId.none)
    OpenGL.glActiveTexture(colorTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, OpenGL.TextureId.none)
    OpenGL.glActiveTexture(normalTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, OpenGL.TextureId.none)
    OpenGL.glActiveTexture(positionTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, OpenGL.TextureId.none)
  }

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
