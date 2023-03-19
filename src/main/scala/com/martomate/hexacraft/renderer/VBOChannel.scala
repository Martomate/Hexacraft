package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.OpenGL

trait VBOChannel {
  def size: Int
  def setAttributes(): Unit
}

case class VBOChannelInt(
    index: Int,
    dims: Int,
    elementSize: Int,
    _type: OpenGL.VertexIntAttributeDataType,
    stride: Int,
    offset: Int,
    divisor: Int
) extends VBOChannel {
  def size: Int = dims * elementSize
  def setAttributes(): Unit = {
    OpenGL.glVertexAttribIPointer(index, dims, _type, stride, offset)
    OpenGL.glVertexAttribDivisor(index, divisor)
    OpenGL.glEnableVertexAttribArray(index)
  }
}

case class VBOChannelFloat(
    index: Int,
    dims: Int,
    elementSize: Int,
    _type: OpenGL.VertexAttributeDataType,
    normalized: Boolean,
    stride: Int,
    offset: Int,
    divisor: Int
) extends VBOChannel {
  def size: Int = dims * elementSize
  def setAttributes(): Unit = {
    OpenGL.glVertexAttribPointer(index, dims, _type, normalized, stride, offset)
    OpenGL.glVertexAttribDivisor(index, divisor)
    OpenGL.glEnableVertexAttribArray(index)
  }
}
