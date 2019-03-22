package com.martomate.hexacraft.renderer

import org.lwjgl.opengl.GL30

import scala.collection.mutable.ArrayBuffer

class VAOBuilder(maxCount: Int, maxPrimCount: Int = 1) {
  private val vbos: ArrayBuffer[VBO] = new ArrayBuffer(1)
  val vaoID: Int = GL30.glGenVertexArrays()
  GL30.glBindVertexArray(vaoID)

  def addVBO(vbo: VBO): VAOBuilder = {
    vbos += vbo
    this
  }

  def create(): VAO = new VAO(vaoID, maxCount, maxPrimCount, vbos)
}
