package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.OpenGL

case class VboAttribute(index: Int, dims: Int, elementSize: Int, format: VboAttribute.Format):
  def width: Int = dims * elementSize

  def upload(offset: Int, stride: Int, divisor: Int): Unit =
    format match
      case VboAttribute.Format.Float =>
        OpenGL.glVertexAttribPointer(index, dims, OpenGL.VertexAttributeDataType.Float, false, stride, offset)
      case VboAttribute.Format.Int =>
        OpenGL.glVertexAttribIPointer(index, dims, OpenGL.VertexIntAttributeDataType.Int, stride, offset)

    OpenGL.glVertexAttribDivisor(index, divisor)
    OpenGL.glEnableVertexAttribArray(index)

object VboAttribute {
  enum Format:
    case Int
    case Float
}
