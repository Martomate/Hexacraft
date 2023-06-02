package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.{OpenGL, Resource}

object VAO {
  private var boundVAO: VAO = _
  def unbindVAO(): Unit = {
    boundVAO = null
    OpenGL.glBindVertexArray(OpenGL.VertexArrayId.none)
  }
}

class VAO(vaoID: OpenGL.VertexArrayId, val maxCount: Int, val maxPrimCount: Int, val vbos: Seq[VBO]) extends Resource {
  def bind(): Unit = {
    if (VAO.boundVAO != this) {
      VAO.boundVAO = this
      OpenGL.glBindVertexArray(vaoID)
    }
  }

  def id: OpenGL.VertexArrayId = vaoID

  protected def reload(): Unit = ()

  protected def unload(): Unit = {
    OpenGL.glDeleteVertexArrays(vaoID)
    vbos.foreach(_.free())
  }
}
