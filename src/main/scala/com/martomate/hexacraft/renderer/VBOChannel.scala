package com.martomate.hexacraft.renderer

import org.lwjgl.opengl.{GL20, GL30, GL33}

trait VBOChannel {
  def size: Int
  def setAttributes(): Unit
}

case class VBOChannelInt(
    index: Int,
    dims: Int,
    elementSize: Int,
    _type: Int,
    stride: Int,
    offset: Int,
    divisor: Int
) extends VBOChannel {
  def size: Int = dims * elementSize
  def setAttributes(): Unit = {
    GL30.glVertexAttribIPointer(index, dims, _type, stride, offset)
    GL33.glVertexAttribDivisor(index, divisor)
    GL20.glEnableVertexAttribArray(index)
  }
}

case class VBOChannelFloat(
    index: Int,
    dims: Int,
    elementSize: Int,
    _type: Int,
    normalized: Boolean,
    stride: Int,
    offset: Int,
    divisor: Int
) extends VBOChannel {
  def size: Int = dims * elementSize
  def setAttributes(): Unit = {
    GL20.glVertexAttribPointer(index, dims, _type, normalized, stride, offset)
    GL33.glVertexAttribDivisor(index, divisor)
    GL20.glEnableVertexAttribArray(index)
  }
}
