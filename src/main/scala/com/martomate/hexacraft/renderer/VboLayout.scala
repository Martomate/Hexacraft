package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.infra.gpu.OpenGL
import scala.collection.mutable.ArrayBuffer

case class VboLayout(attributes: Seq[VboAttribute]) {
  val stride: Int = attributes.map(_.width).sum

  def upload(divisor: Int): Unit =
    var offset = 0
    for ch <- attributes do
      ch.upload(offset, stride, divisor)
      offset += ch.width
}

object VboLayout {
  opaque type Builder <: Any = ArrayBuffer[VboAttribute]

  def builder(): Builder = ArrayBuffer.empty

  extension (channels: Builder)
    def ints(index: Int, dims: Int): Builder =
      channels += VboAttribute(index, dims, 4, VboAttribute.Format.Int)
      channels

    def floats(index: Int, dims: Int): Builder =
      channels += VboAttribute(index, dims, 4, VboAttribute.Format.Float)
      channels

    def floatsArray(index: Int, dims: Int)(size: Int): Builder =
      for i <- 0 until size do channels.floats(index + i, dims)
      channels

    def build(): VboLayout = VboLayout(channels.toSeq)
}
