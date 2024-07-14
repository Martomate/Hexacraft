package hexacraft.renderer

import hexacraft.infra.gpu.OpenGL
import hexacraft.util.Resource

import org.lwjgl.BufferUtils

import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer

object VAO {
  private var boundVAO: VAO = null.asInstanceOf[VAO]

  def unbindVAO(): Unit = {
    boundVAO = null
    OpenGL.glBindVertexArray(OpenGL.VertexArrayId.none)
  }

  private case class VboTemplate(
      count: Int,
      divisor: Int,
      vboUsage: OpenGL.VboUsage,
      layout: VboLayout,
      fillVbo: VBO => Any
  )

  def build(maxCount: Int)(f: Builder => Any): VAO = {
    val b = Builder()
    f(b)
    b.finish(maxCount)
  }

  class Builder private[VAO] {
    private val vboTemplates: ArrayBuffer[VboTemplate] = new ArrayBuffer(1)

    def addVertexVbo(count: Int, vboUsage: OpenGL.VboUsage = OpenGL.VboUsage.StaticDraw)(
        buildLayout: VboLayout.Builder => VboLayout.Builder,
        fillVbo: VBO => Any = _ => ()
    ): Builder = {
      addVbo(count, vboUsage, 0)(buildLayout, fillVbo)
    }

    def addInstanceVbo(count: Int, vboUsage: OpenGL.VboUsage = OpenGL.VboUsage.StaticDraw)(
        buildLayout: VboLayout.Builder => VboLayout.Builder,
        fillVbo: VBO => Any = _ => ()
    ): Builder = {
      addVbo(count, vboUsage, 1)(buildLayout, fillVbo)
    }

    private def addVbo(count: Int, vboUsage: OpenGL.VboUsage, divisor: Int)(
        buildLayout: VboLayout.Builder => VboLayout.Builder,
        fillVbo: VBO => Any
    ): Builder = {
      this.vboTemplates += VboTemplate(count, divisor, vboUsage, VboLayout.build(buildLayout), fillVbo)
      this
    }

    private[VAO] def finish(maxCount: Int): VAO = {
      val vaoID: OpenGL.VertexArrayId = OpenGL.glGenVertexArrays()
      OpenGL.glBindVertexArray(vaoID)

      val vbos = ArrayBuffer.empty[VBO]

      for VboTemplate(count, divisor, vboUsage, layout, fillVbo) <- this.vboTemplates do {
        val vboID: OpenGL.VertexBufferId = OpenGL.glGenBuffers()
        val vbo = new VBO(vboID, layout.stride, vboUsage)

        vbo.resize(count)
        layout.upload(divisor)
        fillVbo(vbo)

        vbos += vbo
      }

      new VAO(vaoID, maxCount, vbos.toSeq)
    }
  }
}

class VAO(val id: OpenGL.VertexArrayId, val maxCount: Int, val vbos: Seq[VBO]) extends Resource {
  def bind(): Unit = {
    if VAO.boundVAO != this then {
      VAO.boundVAO = this
      OpenGL.glBindVertexArray(id)
    }
  }

  protected def unload(): Unit = {
    OpenGL.glDeleteVertexArrays(id)
    for vbo <- vbos do {
      vbo.unload()
    }
  }
}

object VBO {
  private var boundVBO: VBO = null.asInstanceOf[VBO]

  def copy(from: VBO, to: VBO, fromOffset: Int, toOffset: Int, length: Int): Unit = {
    import OpenGL.VertexBufferTarget.*

    OpenGL.glBindBuffer(CopyReadBuffer, from.id)
    OpenGL.glBindBuffer(CopyWriteBuffer, to.id)
    OpenGL.glCopyBufferSubData(CopyReadBuffer, CopyWriteBuffer, fromOffset, toOffset, length)
  }
}

class VBO(private val id: OpenGL.VertexBufferId, val stride: Int, vboUsage: OpenGL.VboUsage) {
  private var count: Int = 0

  def capacity: Int = count

  def bind(): Unit = {
    if VBO.boundVBO != this then {
      VBO.boundVBO = this
      OpenGL.glBindBuffer(OpenGL.VertexBufferTarget.ArrayBuffer, id)
    }
  }

  def resize(newCount: Int): Unit = {
    count = newCount
    bind()
    OpenGL.glBufferData(OpenGL.VertexBufferTarget.ArrayBuffer, count * stride, vboUsage)
  }

  def fill(start: Int, content: ByteBuffer): VBO = {
    bind()
    OpenGL.glBufferSubData(OpenGL.VertexBufferTarget.ArrayBuffer, start * stride, content)
    this
  }

  def fillFloats(start: Int, content: collection.Seq[Float]): VBO = {
    val buf = BufferUtils.createByteBuffer(content.size * 4)
    for f <- content do {
      buf.putFloat(f)
    }
    buf.flip()
    fill(start, buf)
  }

  def fillInts(start: Int, content: collection.Seq[Int]): VBO = {
    val buf = BufferUtils.createByteBuffer(content.size * 4)
    for i <- content do {
      buf.putInt(i)
    }
    buf.flip()
    fill(start, buf)
  }

  def fill(start: Int, content: collection.Seq[VertexData]): VBO = {
    if content.nonEmpty
    then {
      val buf = BufferUtils.createByteBuffer(content.size * content.head.bytesPerVertex)
      for d <- content do {
        d.fill(buf)
      }
      buf.flip()
      fill(start, buf)
    } else this
  }

  def unload(): Unit = {
    OpenGL.glDeleteBuffers(id)
  }
}

trait VertexData {
  def bytesPerVertex: Int

  def fill(buf: ByteBuffer): Unit
}

case class VboLayout(attributes: Seq[VboAttribute]) {
  val stride: Int = attributes.map(_.width).sum

  def upload(divisor: Int): Unit = {
    var offset = 0
    for ch <- attributes do {
      ch.upload(offset, stride, divisor)
      offset += ch.width
    }
  }
}

object VboLayout {
  def build(f: Builder => Any): VboLayout = {
    val b = Builder()
    f(b)
    b.build()
  }

  class Builder private[VboLayout] {
    private val attributes: ArrayBuffer[VboAttribute] = ArrayBuffer.empty

    def ints(index: Int, dims: Int): Builder = {
      this.attributes += VboAttribute(index, dims, 4, VboAttribute.Format.Int)
      this
    }

    def floats(index: Int, dims: Int): Builder = {
      this.attributes += VboAttribute(index, dims, 4, VboAttribute.Format.Float)
      this
    }

    def floatsArray(index: Int, dims: Int)(size: Int): Builder = {
      for i <- 0 until size do this.floats(index + i, dims)
      this
    }

    private[VboLayout] def build(): VboLayout = VboLayout(this.attributes.toSeq)
  }
}

case class VboAttribute(index: Int, dims: Int, elementSize: Int, format: VboAttribute.Format) {
  def width: Int = dims * elementSize

  def upload(offset: Int, stride: Int, divisor: Int): Unit = {
    format match {
      case VboAttribute.Format.Float =>
        OpenGL.glVertexAttribPointer(index, dims, OpenGL.VertexAttributeDataType.Float, false, stride, offset)
      case VboAttribute.Format.Int =>
        OpenGL.glVertexAttribIPointer(index, dims, OpenGL.VertexIntAttributeDataType.Int, stride, offset)
    }

    OpenGL.glVertexAttribDivisor(index, divisor)
    OpenGL.glEnableVertexAttribArray(index)
  }
}

object VboAttribute {
  enum Format {
    case Int
    case Float
  }
}
