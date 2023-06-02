package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.OpenGL

import scala.collection.mutable.ArrayBuffer

class VAOBuilder(maxCount: Int, maxPrimCount: Int = 1) {
  private val vbos: ArrayBuffer[VBO] = new ArrayBuffer(1)
  val vaoID: OpenGL.VertexArrayId = OpenGL.glGenVertexArrays()
  OpenGL.glBindVertexArray(vaoID)

  def addVBO(vbo: VBO): VAOBuilder =
    vbos += vbo
    this

  def create(): VAO = new VAO(vaoID, maxCount, maxPrimCount, vbos.toSeq)
}
