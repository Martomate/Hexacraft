package hexacraft.renderer

import hexacraft.infra.gpu.{OpenGL, VboUsage, VertexBufferLayout}
import hexacraft.util.Resource

import org.lwjgl.BufferUtils

import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer

object VAO {
  private var boundVAO: VAO = null.asInstanceOf[VAO]

  def unbindVAO(): Unit = {
    boundVAO = null
    OpenGL.unbindVertexArray()
  }

  private case class VboTemplate(
      count: Int,
      vboUsage: VboUsage,
      layout: VertexBufferLayout,
      fillVbo: VBO => Any
  )

  def build(maxCount: Int)(f: Builder => Any): VAO = {
    val b = Builder()
    f(b)
    b.finish(maxCount)
  }

  class Builder private[VAO] {
    private val vboTemplates: ArrayBuffer[VboTemplate] = new ArrayBuffer(1)

    def addVertexVbo(count: Int, vboUsage: VboUsage = VboUsage.StaticDraw)(
        buildLayout: VertexBufferLayout.Builder => VertexBufferLayout.Builder,
        fillVbo: VBO => Any = _ => ()
    ): Builder = {
      addVbo(count, vboUsage, false)(buildLayout, fillVbo)
    }

    def addInstanceVbo(count: Int, vboUsage: VboUsage = VboUsage.StaticDraw)(
        buildLayout: VertexBufferLayout.Builder => VertexBufferLayout.Builder,
        fillVbo: VBO => Any = _ => ()
    ): Builder = {
      addVbo(count, vboUsage, true)(buildLayout, fillVbo)
    }

    private def addVbo(count: Int, vboUsage: VboUsage, instanced: Boolean)(
        buildLayout: VertexBufferLayout.Builder => VertexBufferLayout.Builder,
        fillVbo: VBO => Any
    ): Builder = {
      this.vboTemplates += VboTemplate(
        count,
        vboUsage,
        VertexBufferLayout.build(buildLayout, instanced),
        fillVbo
      )
      this
    }

    private[VAO] def finish(maxCount: Int): VAO = {
      val bufferLayouts = this.vboTemplates.map(_.layout).toSeq

      val (vaoId, vboIds) = OpenGL.createVertexArray(bufferLayouts)

      val vbos = for (VboTemplate(count, vboUsage, layout, fillVbo), vboId) <- this.vboTemplates.zip(vboIds) yield {
        val vbo = new VBO(vboId, layout.stride, vboUsage)
        vbo.resize(count)
        fillVbo(vbo)
        vbo
      }

      new VAO(vaoId, maxCount, vbos.toSeq)
    }
  }
}

class VAO(val id: OpenGL.VertexArrayId, val maxCount: Int, val vbos: Seq[VBO]) extends Resource {
  def bind(): Unit = {
    if VAO.boundVAO != this then {
      VAO.boundVAO = this
      OpenGL.bindVertexArray(id)
    }
  }

  protected def unload(): Unit = {
    OpenGL.deleteVertexArray(id)
    for vbo <- vbos do {
      vbo.unload()
    }
  }
}

class VBO(private val id: OpenGL.VertexBufferId, val stride: Int, vboUsage: VboUsage) {
  private var count: Int = 0

  def capacity: Int = count

  def resize(newCount: Int): Unit = {
    count = newCount
    OpenGL.reallocateVertexBuffer(id, count * stride, vboUsage)
  }

  def fill(start: Int, content: ByteBuffer): VBO = {
    OpenGL.writeVertexBufferData(id, start * stride, content)
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
    OpenGL.deleteVertexBuffer(id)
  }
}

trait VertexData {
  def bytesPerVertex: Int

  def fill(buf: ByteBuffer): Unit
}
