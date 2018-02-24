package hexacraft.renderer

import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL33
import hexacraft.resource.Resource

class VBOBuilder(val count: Int, val vboUsage: Int, val divisor: Int) {
  val vboID: Int = GL15.glGenBuffers()
  GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID)

  private trait SomeChannel
  private case class IntChannel(index: Int, dims: Int, elementSize: Int, _type: Int, offset: Int) extends SomeChannel
  private case class FloatChannel(index: Int, dims: Int, elementSize: Int, _type: Int, normalized: Boolean, offset: Int) extends SomeChannel

  private val channels = ArrayBuffer.empty[SomeChannel]
  private var totalStride = 0

  def ints(index: Int, dims: Int, elementSize: Int = 4, _type: Int = GL11.GL_INT): VBOBuilder = {
    channels += IntChannel(index, dims, elementSize, _type, totalStride)
    totalStride += dims * elementSize
    this
  }

  def floats(index: Int, dims: Int, elementSize: Int = 4, _type: Int = GL11.GL_FLOAT, normalized: Boolean = false): VBOBuilder = {
    channels += FloatChannel(index, dims, elementSize, _type, normalized, totalStride)
    totalStride += dims * elementSize
    this
  }

  def create(): VBO = {
    val realChannels = channels.map {
      case IntChannel(index, dims, elementSize, _type, offset) =>
        VBOChannelInt(index, dims, elementSize, _type, totalStride, offset, divisor)
      case FloatChannel(index, dims, elementSize, _type, normalized, offset) =>
        VBOChannelFloat(index, dims, elementSize, _type, normalized, totalStride, offset, divisor)
    }
    val vbo = new VBO(vboID, count, totalStride, vboUsage, realChannels)
    vbo
  }
}

object VBO {
  def apply(count: Int, vboUsage: Int = GL15.GL_STATIC_DRAW, divisor: Int = 0): VBOBuilder = new VBOBuilder(count, vboUsage, divisor)

  private var boundVBO: VBO = _
}

class VBO(vboID: Int, init_count: Int, val stride: Int, val vboUsage: Int, channels: Seq[VBOChannel]) extends Resource {
  var _count: Int = init_count
  def count: Int = _count
  
  bind()
  channels.foreach(_.setAttributes(this))
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

  def fillFloats(start: Int, content: Seq[Float]): VBO = {
    val buf = BufferUtils.createByteBuffer(content.size * 4)
    content.foreach(buf.putFloat)
    buf.flip()
    fill(start, buf)
  }

  def fillInts(start: Int, content: Seq[Int]): VBO = {
    val buf = BufferUtils.createByteBuffer(content.size * 4)
    content.foreach(buf.putInt)
    buf.flip()
    fill(start, buf)
  }

  def unload(): Unit = {
    GL15.glDeleteBuffers(vboID)
  }
}

trait VBOChannel {
  def size: Int
  def setAttributes(vbo: VBO): Unit
}

case class VBOChannelInt(index: Int, dims: Int, elementSize: Int, _type: Int, stride: Int, offset: Int, divisor: Int) extends VBOChannel {
  def size: Int = dims * elementSize
  def setAttributes(vbo: VBO): Unit = {
    GL30.glVertexAttribIPointer(index, dims, _type, stride, offset)
    GL33.glVertexAttribDivisor(index, divisor)
    GL20.glEnableVertexAttribArray(index)
  }
}

case class VBOChannelFloat(index: Int, dims: Int, elementSize: Int, _type: Int, normalized: Boolean, stride: Int, offset: Int, divisor: Int) 
extends VBOChannel {
  def size: Int = dims * elementSize
  def setAttributes(vbo: VBO): Unit = {
    GL20.glVertexAttribPointer(index, dims, _type, normalized, stride, offset)
    GL33.glVertexAttribDivisor(index, divisor)
    GL20.glEnableVertexAttribArray(index)
  }
}
