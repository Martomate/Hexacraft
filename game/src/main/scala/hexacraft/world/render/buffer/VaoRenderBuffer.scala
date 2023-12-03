package hexacraft.world.render.buffer

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{InstancedRenderer, VAO, VBO}
import hexacraft.world.render.BlockVao

import java.nio.ByteBuffer

object VaoRenderBuffer {
  class Allocator(side: Int) extends RenderBuffer.Allocator[VaoRenderBuffer] {
    override def allocate(instances: Int): VaoRenderBuffer =
      val vao = BlockVao.forSide(side)(instances)
      new VaoRenderBuffer(vao, vao.vbos(1), new InstancedRenderer(OpenGL.PrimitiveMode.Triangles))

    override def copy(from: VaoRenderBuffer, to: VaoRenderBuffer, fromIdx: Int, toIdx: Int, len: Int): Unit =
      VBO.copy(from.vboToFill, to.vboToFill, fromIdx, toIdx, len)
  }
}

class VaoRenderBuffer(vao: VAO, val vboToFill: VBO, renderer: InstancedRenderer) extends RenderBuffer[VaoRenderBuffer] {
  override def set(start: Int, buf: ByteBuffer): Unit =
    val vbo = vboToFill
    vbo.fill(start / vbo.stride, buf)

  def render(length: Int): Unit = renderer.render(vao, length / vboToFill.stride)

  override def unload(): Unit = vao.free()
}
