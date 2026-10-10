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
  shader.setUniform1i("translucentPositionTexture", 4)
  shader.setUniform1i("translucentNormalTexture", 5)
  shader.setUniform1i("translucentColorTexture", 6)

  WaterFog.setConstants(shader)

  private val textureSlots: IndexedSeq[OpenGL.TextureSlot] = (0 until 7).map(OpenGL.TextureSlot.ofSlot)

  /** Binds the G-buffer textures of the opaque things and of the translucent things (which are drawn on top) */
  def bindTextures(
      positionTexture: TextureId,
      normalTexture: TextureId,
      colorTexture: TextureId,
      depthTexture: TextureId,
      translucentPositionTexture: TextureId,
      translucentNormalTexture: TextureId,
      translucentColorTexture: TextureId
  ): Unit = {
    val textures = Seq(
      positionTexture,
      normalTexture,
      colorTexture,
      depthTexture,
      translucentPositionTexture,
      translucentNormalTexture,
      translucentColorTexture
    )
    for (slot, texture) <- textureSlots.zip(textures) do {
      OpenGL.glActiveTexture(slot)
      OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, texture)
    }
  }

  def unbindTextures(): Unit = {
    for slot <- textureSlots.reverse do {
      OpenGL.glActiveTexture(slot)
      OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, OpenGL.TextureId.none)
    }
  }

  def setClipPlanes(nearPlane: Float, farPlane: Float): Unit = {
    shader.setUniform1f("nearPlane", nearPlane)
    shader.setUniform1f("farPlane", farPlane)
  }

  def setSunPosition(sun: Vector3f): Unit = {
    shader.setUniform3f("sun", sun.x, sun.y, sun.z)
  }

  def setTotalSize(totalSize: Int): Unit = {
    shader.setUniform1i("totalSize", totalSize)
  }

  /** @param surfaceAboveEye the height of the water surface relative to the eye
    * @param strength how strong the water fog should be (0 means no fog)
    */
  def setWaterSurface(surfaceAboveEye: Float, strength: Float): Unit = {
    WaterFog.setSurface(shader, surfaceAboveEye, strength)
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
