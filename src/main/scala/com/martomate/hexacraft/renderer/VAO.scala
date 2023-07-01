package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.infra.OpenGL
import com.martomate.hexacraft.util.Resource

import scala.collection.mutable.ArrayBuffer

object VAO {
  private var boundVAO: VAO = _
  def unbindVAO(): Unit =
    boundVAO = null
    OpenGL.glBindVertexArray(OpenGL.VertexArrayId.none)

  private case class VboTemplate(
      count: Int,
      divisor: Int,
      vboUsage: OpenGL.VboUsage,
      layout: VboLayout,
      fillVbo: VBO => Any
  )

  opaque type Builder <: Any = ArrayBuffer[VboTemplate]
  def builder(): Builder = new ArrayBuffer(1)

  extension (vboTemplates: Builder)
    def addVertexVbo(count: Int, vboUsage: OpenGL.VboUsage = OpenGL.VboUsage.StaticDraw)(
        buildLayout: VboLayout.Builder => VboLayout.Builder,
        fillVbo: VBO => Any = _ => ()
    ): Builder = addVbo(count, vboUsage, 0)(buildLayout, fillVbo)

    def addInstanceVbo(count: Int, vboUsage: OpenGL.VboUsage = OpenGL.VboUsage.StaticDraw)(
        buildLayout: VboLayout.Builder => VboLayout.Builder,
        fillVbo: VBO => Any = _ => ()
    ): Builder = addVbo(count, vboUsage, 1)(buildLayout, fillVbo)

    private def addVbo(count: Int, vboUsage: OpenGL.VboUsage, divisor: Int)(
        buildLayout: VboLayout.Builder => VboLayout.Builder,
        fillVbo: VBO => Any
    ): Builder =
      val layoutBuilder = VboLayout.builder()
      buildLayout(layoutBuilder)
      vboTemplates += VboTemplate(count, divisor, vboUsage, layoutBuilder.build(), fillVbo)
      vboTemplates

    def finish(maxCount: Int, maxPrimCount: Int = 1): VAO =
      val vaoID: OpenGL.VertexArrayId = OpenGL.glGenVertexArrays()
      OpenGL.glBindVertexArray(vaoID)

      val vbos = ArrayBuffer.empty[VBO]

      for VboTemplate(count, divisor, vboUsage, layout, fillVbo) <- vboTemplates do
        val vboID: OpenGL.VertexBufferId = OpenGL.glGenBuffers()
        val vbo = new VBO(vboID, layout.stride, vboUsage)

        vbo.resize(count)
        layout.upload(divisor)
        fillVbo(vbo)

        vbos += vbo

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
