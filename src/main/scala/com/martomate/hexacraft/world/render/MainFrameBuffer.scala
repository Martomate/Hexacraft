package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.infra.gpu.OpenGL
import com.martomate.hexacraft.renderer.{FrameBuffer, TextureSingle}

import java.nio.{ByteBuffer, FloatBuffer}

object MainFrameBuffer:
  def fromSize(width: Int, height: Int): MainFrameBuffer =
    val colorTexture = Helpers.makeMainColorTexture(width, height)
    val depthTexture = Helpers.makeMainDepthTexture(width, height)
    val frameBuffer = Helpers.makeMainFrameBuffer(colorTexture, depthTexture, width, height)
    new MainFrameBuffer(colorTexture, depthTexture, frameBuffer)

  private object Helpers:
    def makeMainColorTexture(framebufferWidth: Int, framebufferHeight: Int): OpenGL.TextureId =
      val texID = OpenGL.glGenTextures()

      TextureSingle.unbind()
      OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, texID)
      OpenGL.glTexImage2D(
        OpenGL.TextureTarget.Texture2D,
        0,
        OpenGL.TextureInternalFormat.Rgba,
        framebufferWidth,
        framebufferHeight,
        0,
        OpenGL.TexelDataFormat.Rgba,
        OpenGL.TexelDataType.UnsignedByte,
        null.asInstanceOf[ByteBuffer]
      )
      OpenGL.glTexParameteri(
        OpenGL.TextureTarget.Texture2D,
        OpenGL.TexIntParameter.MagFilter(OpenGL.TexMagFilter.Linear)
      )
      OpenGL.glTexParameteri(
        OpenGL.TextureTarget.Texture2D,
        OpenGL.TexIntParameter.MinFilter(OpenGL.TexMinFilter.Linear)
      )

      texID

    def makeMainDepthTexture(framebufferWidth: Int, framebufferHeight: Int): OpenGL.TextureId =
      val texID = OpenGL.glGenTextures()

      TextureSingle.unbind()
      OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, texID)
      OpenGL.glTexImage2D(
        OpenGL.TextureTarget.Texture2D,
        0,
        OpenGL.TextureInternalFormat.DepthComponent32,
        framebufferWidth,
        framebufferHeight,
        0,
        OpenGL.TexelDataFormat.DepthComponent,
        OpenGL.TexelDataType.Float,
        null.asInstanceOf[FloatBuffer]
      )
      OpenGL.glTexParameteri(
        OpenGL.TextureTarget.Texture2D,
        OpenGL.TexIntParameter.MagFilter(OpenGL.TexMagFilter.Linear)
      )
      OpenGL.glTexParameteri(
        OpenGL.TextureTarget.Texture2D,
        OpenGL.TexIntParameter.MinFilter(OpenGL.TexMinFilter.Linear)
      )

      texID

    def makeMainFrameBuffer(
        colorTexture: OpenGL.TextureId,
        depthTexture: OpenGL.TextureId,
        framebufferWidth: Int,
        framebufferHeight: Int
    ): FrameBuffer =
      val fb = new FrameBuffer(framebufferWidth, framebufferHeight)
      fb.bind()
      OpenGL.glDrawBuffer(OpenGL.FrameBufferAttachment.ColorAttachment(0))
      OpenGL.glFramebufferTexture(
        OpenGL.FrameBufferTarget.Regular,
        OpenGL.FrameBufferAttachment.ColorAttachment(0),
        colorTexture,
        0
      )
      OpenGL.glFramebufferTexture(
        OpenGL.FrameBufferTarget.Regular,
        OpenGL.FrameBufferAttachment.DepthAttachment,
        depthTexture,
        0
      )

      fb

class MainFrameBuffer private (
    val colorTexture: OpenGL.TextureId,
    val depthTexture: OpenGL.TextureId,
    val frameBuffer: FrameBuffer
):
  def bind(): Unit =
    frameBuffer.bind()

  def unbind(): Unit =
    frameBuffer.unbind()

  def unload(): Unit =
    frameBuffer.unload()
    OpenGL.glDeleteTextures(colorTexture)
    OpenGL.glDeleteTextures(depthTexture)
