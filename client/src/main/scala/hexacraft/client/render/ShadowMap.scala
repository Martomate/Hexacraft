package hexacraft.client.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{FrameBuffer, TextureArray}

import java.nio.ByteBuffer

/** A depth texture array with one layer per shadow cascade, and a frame buffer for rendering into each layer */
class ShadowMap(val resolution: Int, val numCascades: Int) {
  val texture: OpenGL.TextureId = {
    import OpenGL.*

    val texID = glGenTextures()

    // this changes which texture array is bound, so the cache in TextureArray has to be reset
    TextureArray.unbind()
    glBindTexture(TextureTarget.Texture2DArray, texID)
    glTexImage3D(
      TextureTarget.Texture2DArray,
      0,
      TextureInternalFormat.DepthComponent32,
      resolution,
      resolution,
      numCascades,
      0,
      TexelDataFormat.DepthComponent,
      TexelDataType.Float,
      null.asInstanceOf[ByteBuffer]
    )

    // Linear filtering together with depth comparison gives hardware PCF (i.e. slightly smoothed shadow edges)
    glTexParameteri(TextureTarget.Texture2DArray, TexIntParameter.MagFilter(TexMagFilter.Linear))
    glTexParameteri(TextureTarget.Texture2DArray, TexIntParameter.MinFilter(TexMinFilter.Linear))
    glTexParameteri(TextureTarget.Texture2DArray, TexIntParameter.TextureWrapS(TexWrap.ClampToEdge))
    glTexParameteri(TextureTarget.Texture2DArray, TexIntParameter.TextureWrapT(TexWrap.ClampToEdge))
    glTexParameteri(TextureTarget.Texture2DArray, TexIntParameter.CompareRefToTexture)
    glTexParameteri(TextureTarget.Texture2DArray, TexIntParameter.CompareFunc(DepthFunc.LessThanOrEqual))
    TextureArray.unbind()

    texID
  }

  private val frameBuffers: IndexedSeq[FrameBuffer] =
    for layer <- 0 until numCascades yield {
      import OpenGL.*

      val fb = new FrameBuffer(resolution, resolution)
      fb.bind()
      glDrawBuffer(FrameBufferAttachment.NoAttachment)
      glFramebufferTextureLayer(FrameBufferTarget.Regular, FrameBufferAttachment.DepthAttachment, texture, 0, layer)
      fb.unbind()
      fb
    }

  /** Binds the frame buffer of the given cascade (and sets the viewport) */
  def bind(cascade: Int): Unit = {
    frameBuffers(cascade).bind()
  }

  def unbind(): Unit = {
    frameBuffers.head.unbind()
  }

  def unload(): Unit = {
    for fb <- frameBuffers do {
      fb.unload()
    }
    OpenGL.glDeleteTextures(texture)
  }
}
