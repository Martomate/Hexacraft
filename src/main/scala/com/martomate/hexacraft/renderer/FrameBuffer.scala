package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.OpenGL

import org.lwjgl.opengl.GL30

class FrameBuffer(val width: Int, val height: Int) {
  private val fbID = OpenGL.glGenFramebuffers()

  def bind(): Unit = {
    TextureSingle.unbind()
    OpenGL.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbID)
    OpenGL.glViewport(0, 0, width, height)
  }

  def unbind(): Unit = {
    OpenGL.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
  }

  def unload(): Unit = OpenGL.glDeleteFramebuffers(fbID)
}
