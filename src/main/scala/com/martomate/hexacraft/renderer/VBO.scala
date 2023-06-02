package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.{OpenGL, Resource}

import java.nio.ByteBuffer
import org.lwjgl.BufferUtils

object VBO {
  private var boundVBO: VBO = _

  def copy(from: VBO, to: VBO, fromOffset: Int, toOffset: Int, length: Int): Unit =
    import OpenGL.VertexBufferTarget.*

    OpenGL.glBindBuffer(CopyReadBuffer, from.vboID)
    OpenGL.glBindBuffer(CopyWriteBuffer, to.vboID)
    OpenGL.glCopyBufferSubData(CopyReadBuffer, CopyWriteBuffer, fromOffset, toOffset, length)
}

class VBO(init_count: Int, val stride: Int, val vboUsage: OpenGL.VboUsage, channels: Seq[RealVboChannel])
    extends Resource {
  private var _count: Int = init_count
  def count: Int = _count

  private val vboID: OpenGL.VertexBufferId = OpenGL.glGenBuffers()
  OpenGL.glBindBuffer(OpenGL.VertexBufferTarget.ArrayBuffer, vboID)

  bind()
  for ch <- channels do setAttributes(ch)

  OpenGL.glBufferData(OpenGL.VertexBufferTarget.ArrayBuffer, bufferSize, vboUsage)

  protected def reload(): Unit = ()

  private def bufferSize: Int = count * stride

  def bind(): Unit =
    if VBO.boundVBO != this then
      VBO.boundVBO = this
      OpenGL.glBindBuffer(OpenGL.VertexBufferTarget.ArrayBuffer, vboID)

  def resize(newCount: Int): Unit =
    _count = newCount
    bind()
    OpenGL.glBufferData(OpenGL.VertexBufferTarget.ArrayBuffer, bufferSize, vboUsage)

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

  private def setAttributes(ch: RealVboChannel): Unit =
    val VboChannelBase(index, dims, elementSize) = ch.base

    ch.channelType match
      case VBOChannelType.FloatChannel(dataType, normalized) =>
        OpenGL.glVertexAttribPointer(index, dims, dataType, normalized, stride, ch.offset)
      case VBOChannelType.IntChannel(dataType) =>
        OpenGL.glVertexAttribIPointer(index, dims, dataType, stride, ch.offset)

    OpenGL.glVertexAttribDivisor(index, ch.divisor)
    OpenGL.glEnableVertexAttribArray(index)

  protected def unload(): Unit = OpenGL.glDeleteBuffers(vboID)
}
