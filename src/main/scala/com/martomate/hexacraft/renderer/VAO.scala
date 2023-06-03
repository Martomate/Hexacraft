package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.{OpenGL, Resource}

import scala.collection.mutable.ArrayBuffer

object VAO {
  private var boundVAO: VAO = _
  def unbindVAO(): Unit =
    boundVAO = null
    OpenGL.glBindVertexArray(OpenGL.VertexArrayId.none)

  private case class VboTemplate(
      count: Int,
      stride: Int,
      vboUsage: OpenGL.VboUsage,
      channels: Seq[RealVboChannel],
      fillVbo: VBO => Any
  )

  opaque type Builder <: Any = ArrayBuffer[VboTemplate]
  def builder(): Builder = new ArrayBuffer(1)

  extension (vboTemplates: Builder)
    def addVBO(count: Int, vboUsage: OpenGL.VboUsage = OpenGL.VboUsage.StaticDraw, divisor: Int = 0)(
        buildVbo: VBO.Builder => VBO.Builder,
        fillVbo: VBO => Any = _ => ()
    ): Builder =
      val builder = VBO.builder()
      buildVbo(builder)
      val (stride, channels) = builder.finish(divisor)
      vboTemplates += VboTemplate(count, stride, vboUsage, channels, fillVbo)
      vboTemplates

    def finish(maxCount: Int, maxPrimCount: Int = 1): VAO =
      val vaoID: OpenGL.VertexArrayId = OpenGL.glGenVertexArrays()
      OpenGL.glBindVertexArray(vaoID)

      val vbos =
        for VboTemplate(count, stride, vboUsage, channels, fillVbo) <- vboTemplates
        yield
          val vbo = VBO.create(count, stride, vboUsage, channels)
          fillVbo(vbo)
          vbo

      new VAO(vaoID, maxCount, maxPrimCount, vbos.toSeq)
}

class VAO(val id: OpenGL.VertexArrayId, val maxCount: Int, val maxPrimCount: Int, val vbos: Seq[VBO]) extends Resource {
  def bind(): Unit =
    if VAO.boundVAO != this then
      VAO.boundVAO = this
      OpenGL.glBindVertexArray(id)

  protected def reload(): Unit = ()

  protected def unload(): Unit =
    OpenGL.glDeleteVertexArrays(id)
    for vbo <- vbos do vbo.unload()
}
