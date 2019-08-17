package com.martomate.hexacraft.renderer

import java.nio.ByteBuffer

import com.martomate.hexacraft.resource.Resource
import com.martomate.hexacraft.world.render.BlockVertexData
import org.lwjgl.BufferUtils
import org.lwjgl.opengl._

object VBO {
  private var boundVBO: VBO = _

  def copy(from: VBO, to: VBO, fromOffset: Int, toOffset: Int, length: Int): Unit = {
    GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, from.vboID)
    GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, to.vboID)
    GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, fromOffset, toOffset, length)
  }
}

class VBO(private val vboID: Int, init_count: Int, val stride: Int, val vboUsage: Int, channels: Seq[VBOChannel]) extends Resource {
  var _count: Int = init_count
  def count: Int = _count
  
  bind()
  channels.foreach(_.setAttributes())
  GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bufferSize, vboUsage)
  
  protected def reload(): Unit = ()
  
  def bufferSize: Int = count * stride

  def bind(): Unit = {
    if (VBO.boundVBO != this) {
      VBO.boundVBO = this
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID)
    }
  }

  def resize(newCount: Int): Unit = {
    _count = newCount
    bind()
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bufferSize, vboUsage)
  }

  def fill(start: Int, content: ByteBuffer): VBO = {
    bind()
    GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, start * stride, content)
    this
  }

  def fillFloats(start: Int, content: Seq[Float]): VBO = fillWith(start, content, 4, _.putFloat)

  def fillInts(start: Int, content: Seq[Int]): VBO = fillWith(start, content, 4, _.putInt)

  def fill(start: Int, content: Seq[VertexData]): VBO =
    if (content.nonEmpty)
      fillWith[VertexData](start, content, content.head.bytesPerVertex, buf => data => data.fill(buf))
    else this

  private def fillWith[T](start: Int, content: Seq[T], tSize: Int, howToFill: ByteBuffer => T => Any): VBO = {
    val buf = BufferUtils.createByteBuffer(content.size * tSize)
    content.foreach(howToFill(buf))
    buf.flip()
    fill(start, buf)
  }

  protected def unload(): Unit = {
    GL15.glDeleteBuffers(vboID)
  }
}
