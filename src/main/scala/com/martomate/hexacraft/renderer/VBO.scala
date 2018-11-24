package com.martomate.hexacraft.renderer

import java.nio.ByteBuffer

import com.martomate.hexacraft.resource.Resource
import com.martomate.hexacraft.world.render.BlockVertexData
import org.lwjgl.BufferUtils
import org.lwjgl.opengl._

import scala.collection.mutable.ArrayBuffer

object VBO {
  private var boundVBO: VBO = _
}

class VBO(vboID: Int, init_count: Int, val stride: Int, val vboUsage: Int, channels: Seq[VBOChannel]) extends Resource {
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

  def fill(start: Int, content: Seq[BlockVertexData]): VBO = fillWith[BlockVertexData](start, content, (3+2+3+1)*4, buf => data => data.fill(buf))

  private def fillWith[T](start: Int, content: Seq[T], tSize: Int, howToFill: ByteBuffer => T => Any): VBO = {
    val buf = BufferUtils.createByteBuffer(content.size * tSize)
    content.foreach(howToFill(buf))
    buf.flip()
    fill(start, buf)
  }

  def unload(): Unit = {
    GL15.glDeleteBuffers(vboID)
  }
}
