package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.{FrameBuffer, TextureSingle}

import java.nio.{ByteBuffer, FloatBuffer}
import org.lwjgl.opengl.{GL11, GL14, GL30, GL32}

object MainFrameBuffer:
  def fromSize(width: Int, height: Int): MainFrameBuffer =
    val colorTexture = Helpers.makeMainColorTexture(width, height)
    val depthTexture = Helpers.makeMainDepthTexture(width, height)
    val frameBuffer = Helpers.makeMainFrameBuffer(colorTexture, depthTexture, width, height)
    new MainFrameBuffer(colorTexture, depthTexture, frameBuffer)

  private object Helpers:
    def makeMainColorTexture(framebufferWidth: Int, framebufferHeight: Int): Int =
      val texID = GL11.glGenTextures()

      TextureSingle.unbind()
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID)
      GL11.glTexImage2D(
        GL11.GL_TEXTURE_2D,
        0,
        GL11.GL_RGBA,
        framebufferWidth,
        framebufferHeight,
        0,
        GL11.GL_RGBA,
        GL11.GL_UNSIGNED_BYTE,
        null.asInstanceOf[ByteBuffer]
      )
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)

      texID

    def makeMainDepthTexture(framebufferWidth: Int, framebufferHeight: Int): Int =
      val texID = GL11.glGenTextures()

      TextureSingle.unbind()
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID)
      GL11.glTexImage2D(
        GL11.GL_TEXTURE_2D,
        0,
        GL14.GL_DEPTH_COMPONENT32,
        framebufferWidth,
        framebufferHeight,
        0,
        GL11.GL_DEPTH_COMPONENT,
        GL11.GL_FLOAT,
        null.asInstanceOf[FloatBuffer]
      )
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)

      texID

    def makeMainFrameBuffer(
        colorTexture: Int,
        depthTexture: Int,
        framebufferWidth: Int,
        framebufferHeight: Int
    ): FrameBuffer =
      val fb = new FrameBuffer(framebufferWidth, framebufferHeight)
      fb.bind()
      GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0)
      GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, colorTexture, 0)
      GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, depthTexture, 0)

      fb

class MainFrameBuffer private (val colorTexture: Int, val depthTexture: Int, val frameBuffer: FrameBuffer):
  def bind(): Unit =
    frameBuffer.bind()

  def unbind(): Unit =
    frameBuffer.unbind()

  def unload(): Unit =
    frameBuffer.unload()
    GL11.glDeleteTextures(colorTexture)
    GL11.glDeleteTextures(depthTexture)
