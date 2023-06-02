package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.OpenGL

case class VboChannelBase(index: Int, dims: Int, elementSize: Int)

enum VBOChannelType:
  case IntChannel(dataType: OpenGL.VertexIntAttributeDataType)
  case FloatChannel(dataType: OpenGL.VertexAttributeDataType, normalized: Boolean)

case class RealVboChannel(base: VboChannelBase, channelType: VBOChannelType, offset: Int, stride: Int, divisor: Int)
