package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.OpenGL

case class VboChannelBase(index: Int, dims: Int, elementSize: Int)

enum VBOChannelType:
  case IntChannel(dataType: OpenGL.VertexIntAttributeDataType)
  case FloatChannel(dataType: OpenGL.VertexAttributeDataType, normalized: Boolean)

case class RealVboChannel(base: VboChannelBase, channelType: VBOChannelType, offset: Int, divisor: Int) {
  def setAttributes(stride: Int): Unit =
    val VboChannelBase(index, dims, elementSize) = base

    channelType match
      case VBOChannelType.FloatChannel(dataType, normalized) =>
        OpenGL.glVertexAttribPointer(index, dims, dataType, normalized, stride, offset)
      case VBOChannelType.IntChannel(dataType) =>
        OpenGL.glVertexAttribIPointer(index, dims, dataType, stride, offset)

    OpenGL.glVertexAttribDivisor(index, divisor)
    OpenGL.glEnableVertexAttribArray(index)
}
