package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.{OpenGL, Resource}

import java.nio.ByteBuffer
import org.lwjgl.BufferUtils
import scala.collection.mutable.ArrayBuffer

object VBO {
  private var boundVBO: VBO = _

  private val IntChannelType = VBOChannelType.IntChannel(OpenGL.VertexIntAttributeDataType.Int)
  private val FloatChannelType = VBOChannelType.FloatChannel(OpenGL.VertexAttributeDataType.Float, false)

  opaque type Builder <: Any = ArrayBuffer[(VboChannelBase, VBOChannelType)]
  def builder(): Builder = ArrayBuffer.empty

  extension (channels: Builder)
    def ints(index: Int, dims: Int): Builder =
      channels += VboChannelBase(index, dims, 4) -> IntChannelType
      channels

    def floats(index: Int, dims: Int): Builder =
      channels += VboChannelBase(index, dims, 4) -> FloatChannelType
      channels

    def floatsArray(index: Int, dims: Int)(size: Int): Builder =
      for i <- 0 until size do channels.floats(index + i, dims)
      channels

    def finish(divisor: Int): (Int, Seq[RealVboChannel]) =
      val realChannels = ArrayBuffer.empty[RealVboChannel]
      var offset = 0

      for (base, info) <- channels
      do
        realChannels += RealVboChannel(base, info, offset, divisor)
        offset += base.dims * base.elementSize

      (offset, realChannels.toSeq)

  def copy(from: VBO, to: VBO, fromOffset: Int, toOffset: Int, length: Int): Unit =
    import OpenGL.VertexBufferTarget.*

    OpenGL.glBindBuffer(CopyReadBuffer, from.id)
    OpenGL.glBindBuffer(CopyWriteBuffer, to.id)
    OpenGL.glCopyBufferSubData(CopyReadBuffer, CopyWriteBuffer, fromOffset, toOffset, length)

  def create(count: Int, stride: Int, vboUsage: OpenGL.VboUsage, channels: Seq[RealVboChannel]): VBO =
    val vboID: OpenGL.VertexBufferId = OpenGL.glGenBuffers()
    val vbo = new VBO(vboID, stride, vboUsage)

    vbo.resize(count)
    for ch <- channels do ch.setAttributes(stride)
    vbo
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
