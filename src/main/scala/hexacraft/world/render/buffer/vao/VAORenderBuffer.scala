package hexacraft.world.render.buffer.vao

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{InstancedRenderer, VAO, VBO}
import hexacraft.world.render.buffer.RenderBuffer

import java.nio.ByteBuffer

object VAORenderBuffer {
  class Allocator(side: Int) extends RenderBuffer.Allocator[VAORenderBuffer] {
    override def allocate(instances: Int): VAORenderBuffer =
      val vao = BlockVao.forSide(side)(instances)
      new VAORenderBuffer(vao, vao.vbos(1), new InstancedRenderer(OpenGL.PrimitiveMode.Triangles))

    override def copy(from: VAORenderBuffer, to: VAORenderBuffer, fromIdx: Int, toIdx: Int, len: Int): Unit =
      VBO.copy(from.vboToFill, to.vboToFill, fromIdx, toIdx, len)
  }
}

class VAORenderBuffer(vao: VAO, val vboToFill: VBO, renderer: InstancedRenderer) extends RenderBuffer[VAORenderBuffer] {
  override def set(start: Int, buf: ByteBuffer): Unit =
    val vbo = vboToFill
    vbo.fill(start / vbo.stride, buf)

  def render(length: Int): Unit = renderer.render(vao, length / vboToFill.stride)

  override def unload(): Unit = vao.free()
}
