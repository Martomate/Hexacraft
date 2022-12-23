package com.martomate.hexacraft.world.render.buffer.vao

import com.martomate.hexacraft.renderer.{InstancedRenderer, Renderer, VAO, VBO}
import com.martomate.hexacraft.world.render.buffer.RenderBuffer

import java.nio.ByteBuffer
import org.lwjgl.opengl.GL11

class VAORenderBuffer(val vao: VAO, val idxToFill: Int, renderingMode: Int) extends RenderBuffer[VAORenderBuffer] {
  private def vboToFill: VBO = vao.vbos(idxToFill)

  protected val renderer: Renderer = new InstancedRenderer(vao, renderingMode)

  override def set(start: Int, length: Int, buf: ByteBuffer): Unit = {
    val lim = buf.limit()
    buf.limit(buf.position() + length)
    val vbo = vboToFill
    vbo.fill(start / vbo.stride, buf)
    buf.position(buf.limit())
    buf.limit(lim)
  }

  override def copyTo(buffer: VAORenderBuffer, fromIdx: Int, toIdx: Int, len: Int): Unit =
    VBO.copy(vboToFill, buffer.vboToFill, fromIdx, toIdx, len)

  def render(length: Int): Unit = renderer.render(length / vboToFill.stride)

  override def unload(): Unit = {
    vao.free()
  }
}
