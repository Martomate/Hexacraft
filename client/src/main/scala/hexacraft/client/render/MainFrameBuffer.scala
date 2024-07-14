package hexacraft.client.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{FrameBuffer, TextureSingle}

import java.nio.{ByteBuffer, FloatBuffer}

object MainFrameBuffer {
  def fromSize(width: Int, height: Int): MainFrameBuffer = {
    val colorTexture = Helpers.makeMainColorTexture(width, height)
    val depthTexture = Helpers.makeMainDepthTexture(width, height)
    val frameBuffer = Helpers.makeMainFrameBuffer(colorTexture, depthTexture, width, height)

    new MainFrameBuffer(colorTexture, depthTexture, frameBuffer)
  }

  private object Helpers {
    def makeMainColorTexture(frameBufferWidth: Int, frameBufferHeight: Int): OpenGL.TextureId = {
      import OpenGL.*

      val texID = glGenTextures()

      TextureSingle.unbind()
      glBindTexture(TextureTarget.Texture2D, texID)
      glTexImage2D(
        TextureTarget.Texture2D,
        0,
        TextureInternalFormat.Rgba,
        frameBufferWidth,
        frameBufferHeight,
        0,
        TexelDataFormat.Rgba,
        TexelDataType.UnsignedByte,
        null.asInstanceOf[ByteBuffer]
      )
      glTexParameteri(TextureTarget.Texture2D, TexIntParameter.MagFilter(TexMagFilter.Linear))
      glTexParameteri(TextureTarget.Texture2D, TexIntParameter.MinFilter(TexMinFilter.Linear))

      texID
    }

    def makeMainDepthTexture(frameBufferWidth: Int, frameBufferHeight: Int): OpenGL.TextureId = {
      import OpenGL.*

      val texID = glGenTextures()
      TextureSingle.unbind()
      glBindTexture(TextureTarget.Texture2D, texID)

      glTexImage2D(
        TextureTarget.Texture2D,
        0,
        TextureInternalFormat.DepthComponent32,
        frameBufferWidth,
        frameBufferHeight,
        0,
        TexelDataFormat.DepthComponent,
        TexelDataType.Float,
        null.asInstanceOf[FloatBuffer]
      )
      glTexParameteri(TextureTarget.Texture2D, TexIntParameter.MagFilter(TexMagFilter.Linear))
      glTexParameteri(TextureTarget.Texture2D, TexIntParameter.MinFilter(TexMinFilter.Linear))

      texID
    }

    def makeMainFrameBuffer(
        colorTexture: OpenGL.TextureId,
        depthTexture: OpenGL.TextureId,
        frameBufferWidth: Int,
        frameBufferHeight: Int
    ): FrameBuffer = {
      import OpenGL.*

      val fb = new FrameBuffer(frameBufferWidth, frameBufferHeight)
      fb.bind()

      glDrawBuffer(FrameBufferAttachment.ColorAttachment(0))
      glFramebufferTexture(FrameBufferTarget.Regular, FrameBufferAttachment.ColorAttachment(0), colorTexture, 0)
      glFramebufferTexture(FrameBufferTarget.Regular, FrameBufferAttachment.DepthAttachment, depthTexture, 0)

      fb
    }
  }
}

class MainFrameBuffer private (
    val colorTexture: OpenGL.TextureId,
    val depthTexture: OpenGL.TextureId,
    val frameBuffer: FrameBuffer
) {
  def bind(): Unit = {
    frameBuffer.bind()
  }

  def unbind(): Unit = {
    frameBuffer.unbind()
  }

  def unload(): Unit = {
    frameBuffer.unload()
    OpenGL.glDeleteTextures(colorTexture)
    OpenGL.glDeleteTextures(depthTexture)
  }
}
