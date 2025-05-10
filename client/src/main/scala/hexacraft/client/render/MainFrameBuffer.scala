package hexacraft.client.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{FrameBuffer, TextureSingle}

import org.joml.{Vector2i, Vector2ic}

import java.nio.{ByteBuffer, FloatBuffer}

object MainFrameBuffer {
  def fromSize(width: Int, height: Int): MainFrameBuffer = {
    val positionTexture = Helpers.makeMainPositionTexture(width, height)
    val normalTexture = Helpers.makeMainNormalTexture(width, height)
    val colorTexture = Helpers.makeMainColorTexture(width, height)
    val depthTexture = Helpers.makeMainDepthTexture(width, height)
    val frameBuffer =
      Helpers.makeMainFrameBuffer(positionTexture, normalTexture, colorTexture, depthTexture, width, height)

    new MainFrameBuffer(positionTexture, normalTexture, colorTexture, depthTexture, frameBuffer)
  }

  private object Helpers {
    def makeMainPositionTexture(frameBufferWidth: Int, frameBufferHeight: Int): OpenGL.TextureId = {
      import OpenGL.*

      val texID = glGenTextures()

      TextureSingle.unbind()
      glBindTexture(TextureTarget.Texture2D, texID)
      glTexImage2D(
        TextureTarget.Texture2D,
        0,
        TextureInternalFormat.Rgba16f,
        frameBufferWidth,
        frameBufferHeight,
        0,
        TexelDataFormat.Rgba,
        TexelDataType.Float,
        null.asInstanceOf[FloatBuffer]
      )
      glTexParameteri(TextureTarget.Texture2D, TexIntParameter.MagFilter(TexMagFilter.Linear))
      glTexParameteri(TextureTarget.Texture2D, TexIntParameter.MinFilter(TexMinFilter.Linear))

      texID
    }

    def makeMainNormalTexture(frameBufferWidth: Int, frameBufferHeight: Int): OpenGL.TextureId = {
      import OpenGL.*

      val texID = glGenTextures()

      TextureSingle.unbind()
      glBindTexture(TextureTarget.Texture2D, texID)
      glTexImage2D(
        TextureTarget.Texture2D,
        0,
        TextureInternalFormat.Rgba16f,
        frameBufferWidth,
        frameBufferHeight,
        0,
        TexelDataFormat.Rgba,
        TexelDataType.Float,
        null.asInstanceOf[FloatBuffer]
      )
      glTexParameteri(TextureTarget.Texture2D, TexIntParameter.MagFilter(TexMagFilter.Linear))
      glTexParameteri(TextureTarget.Texture2D, TexIntParameter.MinFilter(TexMinFilter.Linear))

      texID
    }

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
        positionTexture: OpenGL.TextureId,
        normalTexture: OpenGL.TextureId,
        colorTexture: OpenGL.TextureId,
        depthTexture: OpenGL.TextureId,
        frameBufferWidth: Int,
        frameBufferHeight: Int
    ): FrameBuffer = {
      import OpenGL.*

      val fb = new FrameBuffer(frameBufferWidth, frameBufferHeight)
      fb.bind()

      glDrawBuffers(
        Seq(
          FrameBufferAttachment.ColorAttachment(0),
          FrameBufferAttachment.ColorAttachment(1),
          FrameBufferAttachment.ColorAttachment(2)
        )
      )
      glFramebufferTexture(FrameBufferTarget.Regular, FrameBufferAttachment.ColorAttachment(0), positionTexture, 0)
      glFramebufferTexture(FrameBufferTarget.Regular, FrameBufferAttachment.ColorAttachment(1), normalTexture, 0)
      glFramebufferTexture(FrameBufferTarget.Regular, FrameBufferAttachment.ColorAttachment(2), colorTexture, 0)
      glFramebufferTexture(FrameBufferTarget.Regular, FrameBufferAttachment.DepthAttachment, depthTexture, 0)

      fb
    }
  }
}

class MainFrameBuffer private (
    val positionTexture: OpenGL.TextureId,
    val normalTexture: OpenGL.TextureId,
    val colorTexture: OpenGL.TextureId,
    val depthTexture: OpenGL.TextureId,
    frameBuffer: FrameBuffer
) {
  def size: Vector2ic = new Vector2i(frameBuffer.width, frameBuffer.height)

  def bind(): Unit = {
    frameBuffer.bind()
  }

  def unbind(): Unit = {
    frameBuffer.unbind()
  }

  def unload(): Unit = {
    frameBuffer.unload()
    OpenGL.glDeleteTextures(positionTexture)
    OpenGL.glDeleteTextures(normalTexture)
    OpenGL.glDeleteTextures(colorTexture)
    OpenGL.glDeleteTextures(depthTexture)
  }
}
