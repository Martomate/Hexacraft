package hexacraft.renderer

import hexacraft.infra.gpu.OpenGL
import hexacraft.util.Resource
import org.lwjgl.BufferUtils

import java.nio.ByteBuffer

object VBO {
  private var boundVBO: VBO = _

  def copy(from: VBO, to: VBO, fromOffset: Int, toOffset: Int, length: Int): Unit =
    import OpenGL.VertexBufferTarget.*

    OpenGL.glBindBuffer(CopyReadBuffer, from.id)
    OpenGL.glBindBuffer(CopyWriteBuffer, to.id)
    OpenGL.glCopyBufferSubData(CopyReadBuffer, CopyWriteBuffer, fromOffset, toOffset, length)
}

class VBO(private val id: OpenGL.VertexBufferId, val stride: Int, vboUsage: OpenGL.VboUsage) {
  private var count: Int = 0

  def bind(): Unit =
    if VBO.boundVBO != this then
      VBO.boundVBO = this
      OpenGL.glBindBuffer(OpenGL.VertexBufferTarget.ArrayBuffer, id)

  def resize(newCount: Int): Unit =
    count = newCount
    bind()
    OpenGL.glBufferData(OpenGL.VertexBufferTarget.ArrayBuffer, count * stride, vboUsage)

  def fill(start: Int, content: ByteBuffer): VBO =
    bind()
    OpenGL.glBufferSubData(OpenGL.VertexBufferTarget.ArrayBuffer, start * stride, content)
    this

  def fillFloats(start: Int, content: collection.Seq[Float]): VBO =
    val buf = BufferUtils.createByteBuffer(content.size * 4)
    for f <- content do buf.putFloat(f)
    buf.flip()
    fill(start, buf)

  def fillInts(start: Int, content: collection.Seq[Int]): VBO =
    val buf = BufferUtils.createByteBuffer(content.size * 4)
    for i <- content do buf.putInt(i)
    buf.flip()
    fill(start, buf)

  def fill(start: Int, content: collection.Seq[VertexData]): VBO =
    if content.nonEmpty
    then
      val buf = BufferUtils.createByteBuffer(content.size * content.head.bytesPerVertex)
      for d <- content do d.fill(buf)
      buf.flip()
      fill(start, buf)
    else this

  def unload(): Unit = OpenGL.glDeleteBuffers(id)
}
