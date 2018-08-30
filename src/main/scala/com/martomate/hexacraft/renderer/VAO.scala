package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.resource.Resource
import org.lwjgl.opengl.GL30

import scala.collection.mutable.ArrayBuffer

object VAO {
  private var boundVAO: VAO = _
  def unbindVAO(): Unit = {
    boundVAO = null
    GL30.glBindVertexArray(0)
  }
}

class VAO(vaoID: Int, val maxCount: Int, val maxPrimCount: Int, val vbos: Seq[VBO]) extends Resource {
  def bind(): Unit = {
    if (VAO.boundVAO != this) {
      VAO.boundVAO = this
      GL30.glBindVertexArray(vaoID)
    }
  }

  def id: Int = vaoID
  
  protected def reload(): Unit = ()

  def unload(): Unit = {
    GL30.glDeleteVertexArrays(vaoID)
    vbos.foreach(_.unload())
  }
}

class VAOBuilder(maxCount: Int, maxPrimCount: Int = 1) {
  private val vbos = ArrayBuffer.empty[VBO]
  val vaoID: Int = GL30.glGenVertexArrays()
  GL30.glBindVertexArray(vaoID)

  def addVBO(vbo: VBO): VAOBuilder = {
    vbos += vbo
    this
  }

  def create(): VAO = new VAO(vaoID, maxCount, maxPrimCount, vbos)
}
