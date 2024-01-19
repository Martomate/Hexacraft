package hexacraft.renderer

import hexacraft.infra.gpu.OpenGL

class FrameBuffer(val width: Int, val height: Int) {
  private val fbID = OpenGL.glGenFramebuffer()

  def bind(): Unit = {
    TextureSingle.unbind()
    OpenGL.glBindFramebuffer(OpenGL.FrameBufferTarget.Regular, fbID)
    OpenGL.glViewport(0, 0, width, height)
  }

  def unbind(): Unit = {
    OpenGL.glBindFramebuffer(OpenGL.FrameBufferTarget.Regular, OpenGL.FrameBufferId.none)
  }

  def unload(): Unit = {
    OpenGL.glDeleteFramebuffer(fbID)
  }
}
