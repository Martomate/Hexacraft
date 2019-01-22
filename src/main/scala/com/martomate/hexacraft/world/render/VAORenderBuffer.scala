package com.martomate.hexacraft.world.render

import java.nio.ByteBuffer

import com.martomate.hexacraft.renderer.{InstancedRenderer, Renderer, VAO, VBO}
import org.lwjgl.opengl.GL11

class VAORenderBuffer(val vao: VAO, val idxToFill: Int) extends RenderBuffer {
  protected val renderer: Renderer = new InstancedRenderer(vao, GL11.GL_TRIANGLE_STRIP)

  def set(start: Int, length: Int, buf: ByteBuffer): Unit = {
    val lim = buf.limit()
    buf.limit(buf.position() + length)
    val vbo = vao.vbos(idxToFill)
    vbo.fill(start / vbo.stride, buf)
    buf.position(buf.limit())
    buf.limit(lim)
  }

  def copyFrom(buffer: VAORenderBuffer, fromIdx: Int, toIdx: Int, len: Int): Unit = VBO.copy(vao.vbos(idxToFill), buffer.vao.vbos(buffer.idxToFill), fromIdx, toIdx, len)

  def render(length: Int): Unit = renderer.render(length)
}
