package hexacraft.world.render.buffer.vao

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{InstancedRenderer, VAO, VBO}
import hexacraft.world.render.buffer.RenderBuffer

import java.nio.ByteBuffer

class VAORenderBuffer(val vao: VAO, val idxToFill: Int, renderingMode: OpenGL.PrimitiveMode)
    extends RenderBuffer[VAORenderBuffer] {
  private def vboToFill: VBO = vao.vbos(idxToFill)

  private val renderer = new InstancedRenderer(renderingMode)

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

  def render(length: Int): Unit = renderer.render(vao, length / vboToFill.stride)

  override def unload(): Unit = {
    vao.free()
  }
}
