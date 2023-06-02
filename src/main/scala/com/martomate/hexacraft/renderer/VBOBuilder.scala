package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.OpenGL

import scala.collection.mutable.ArrayBuffer

object VBOBuilder {
  def apply(count: Int, vboUsage: OpenGL.VboUsage = OpenGL.VboUsage.StaticDraw, divisor: Int = 0): VBOBuilder =
    new VBOBuilder(count, vboUsage, divisor)
}

class VBOBuilder(val count: Int, val vboUsage: OpenGL.VboUsage, val divisor: Int) {
  val vboID: OpenGL.VertexBufferId = OpenGL.glGenBuffers()
  OpenGL.glBindBuffer(OpenGL.VertexBufferTarget.ArrayBuffer, vboID)

  private sealed trait SomeChannel
  private case class IntChannel(
      index: Int,
      dims: Int,
      elementSize: Int,
      _type: OpenGL.VertexIntAttributeDataType,
      offset: Int
  ) extends SomeChannel
  private case class FloatChannel(
      index: Int,
      dims: Int,
      elementSize: Int,
      _type: OpenGL.VertexAttributeDataType,
      normalized: Boolean,
      offset: Int
  ) extends SomeChannel

  private val channels = ArrayBuffer.empty[SomeChannel]
  private var totalStride = 0

  def ints(
      index: Int,
      dims: Int,
      elementSize: Int = 4,
      _type: OpenGL.VertexIntAttributeDataType = OpenGL.VertexIntAttributeDataType.Int
  ): VBOBuilder = {
    channels += IntChannel(index, dims, elementSize, _type, totalStride)
    totalStride += dims * elementSize
    this
  }

  def floats(
      index: Int,
      dims: Int,
      elementSize: Int = 4,
      _type: OpenGL.VertexAttributeDataType = OpenGL.VertexAttributeDataType.Float,
      normalized: Boolean = false
  ): VBOBuilder = {
    channels += FloatChannel(index, dims, elementSize, _type, normalized, totalStride)
    totalStride += dims * elementSize
    this
  }

  def intsArray(
      index: Int,
      dims: Int,
      elementSize: Int = 4,
      _type: OpenGL.VertexIntAttributeDataType = OpenGL.VertexIntAttributeDataType.Int
  )(
      size: Int
  ): VBOBuilder = {
    for (i <- 0 until size) {
      ints(index + i, dims, elementSize, _type)
    }
    this
  }

  def floatsArray(
      index: Int,
      dims: Int,
      elementSize: Int = 4,
      _type: OpenGL.VertexAttributeDataType = OpenGL.VertexAttributeDataType.Float,
      normalized: Boolean = false
  )(size: Int): VBOBuilder = {
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
