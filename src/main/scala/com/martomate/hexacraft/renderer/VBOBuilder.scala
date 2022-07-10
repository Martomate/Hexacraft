package com.martomate.hexacraft.renderer

import org.lwjgl.opengl.{GL11, GL15}

import scala.collection.mutable.ArrayBuffer

object VBOBuilder {
  def apply(count: Int, vboUsage: Int = GL15.GL_STATIC_DRAW, divisor: Int = 0): VBOBuilder = new VBOBuilder(count, vboUsage, divisor)
}

class VBOBuilder(val count: Int, val vboUsage: Int, val divisor: Int) {
  val vboID: Int = GL15.glGenBuffers()
  GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID)

  private sealed trait SomeChannel
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

  def intsArray(index: Int, dims: Int, elementSize: Int = 4, _type: Int = GL11.GL_INT)(size: Int): VBOBuilder = {
    for (i <- 0 until size) {
      ints(index + i, dims, elementSize, _type)
    }
    this
  }

  def floatsArray(index: Int, dims: Int, elementSize: Int = 4, _type: Int = GL11.GL_FLOAT, normalized: Boolean = false)(size: Int): VBOBuilder = {
    for (i <- 0 until size) {
      floats(index + i, dims, elementSize, _type, normalized)
    }
    this
  }

  def create(): VBO = {
    val realChannels = channels.map {
      case IntChannel(index, dims, elementSize, _type, offset) =>
        VBOChannelInt(index, dims, elementSize, _type, totalStride, offset, divisor)
      case FloatChannel(index, dims, elementSize, _type, normalized, offset) =>
        VBOChannelFloat(index, dims, elementSize, _type, normalized, totalStride, offset, divisor)
    }
    val vbo = new VBO(vboID, count, totalStride, vboUsage, realChannels.toSeq)
    vbo
  }
}
