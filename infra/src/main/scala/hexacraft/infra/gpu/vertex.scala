package hexacraft.infra.gpu

import scala.collection.mutable.ArrayBuffer

object VertexBufferLayout {
  def build(f: Builder => Any, instanced: Boolean): VertexBufferLayout = {
    val b = new Builder
    f(b)
    b.build(instanced)
  }

  class Builder private[VertexBufferLayout] {
    private val attributes: ArrayBuffer[VertexAttribute] = ArrayBuffer.empty

    def ints(index: Int, dims: Int): Builder = {
      this.attributes += VertexAttribute(index, dims, 4, VertexAttribute.DataType.Int)
      this
    }

    def floats(index: Int, dims: Int): Builder = {
      this.attributes += VertexAttribute(index, dims, 4, VertexAttribute.DataType.Float)
      this
    }

    def floatsArray(index: Int, dims: Int)(size: Int): Builder = {
      for i <- 0 until size do this.floats(index + i, dims)
      this
    }

    private[VertexBufferLayout] def build(instanced: Boolean): VertexBufferLayout =
      VertexBufferLayout(instanced, this.attributes.toSeq)
  }
}

case class VertexBufferLayout(instanced: Boolean, attributes: Seq[VertexAttribute]) {
  val stride = attributes.map(_.width).sum
}

case class VertexAttribute(index: Int, dims: Int, elementSize: Int, dataType: VertexAttribute.DataType) {
  val width: Int = dims * elementSize
}

object VertexAttribute {
  enum DataType {
    case Int
    case Float
  }
}

enum VboUsage {
  case StaticDraw
  case DynamicDraw
}
