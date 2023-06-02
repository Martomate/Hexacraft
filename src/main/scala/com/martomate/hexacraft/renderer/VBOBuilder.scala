package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.OpenGL

import scala.collection.mutable.ArrayBuffer

case class VBOBuilder() {
  private val channels = ArrayBuffer.empty[(VboChannelBase, VBOChannelType)]

  private val IntChannelType = VBOChannelType.IntChannel(OpenGL.VertexIntAttributeDataType.Int)
  private val FloatChannelType = VBOChannelType.FloatChannel(OpenGL.VertexAttributeDataType.Float, false)

  def ints(index: Int, dims: Int): VBOBuilder =
    channels += VboChannelBase(index, dims, 4) -> IntChannelType
    this

  def floats(index: Int, dims: Int): VBOBuilder =
    channels += VboChannelBase(index, dims, 4) -> FloatChannelType
    this

  def floatsArray(index: Int, dims: Int)(size: Int): VBOBuilder =
    for i <- 0 until size do floats(index + i, dims)
    this

  def create(count: Int, vboUsage: OpenGL.VboUsage = OpenGL.VboUsage.StaticDraw, divisor: Int = 0): VBO =
    val totalStride = channels.map((base, _) => base.dims * base.elementSize).sum
    val realChannels = ArrayBuffer.empty[RealVboChannel]
    var offset = 0

    for (base, info) <- channels
    do
      realChannels += RealVboChannel(base, info, offset, totalStride, divisor)
      offset += base.dims * base.elementSize

    new VBO(count, totalStride, vboUsage, realChannels.toSeq)
}
