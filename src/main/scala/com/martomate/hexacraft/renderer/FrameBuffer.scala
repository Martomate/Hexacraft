package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.resource.TextureSingle
import org.lwjgl.opengl.{GL11, GL30}

class FrameBuffer(val width: Int, val height: Int) {
  private val fbID = GL30.glGenFramebuffers()

  def bind(): Unit = {
    TextureSingle.unbind()
    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbID)
    GL11.glViewport(0, 0, width, height)
  }

  def unbind(): Unit = {
    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
  }

  def unload(): Unit = GL30.glDeleteFramebuffers(fbID)
}
