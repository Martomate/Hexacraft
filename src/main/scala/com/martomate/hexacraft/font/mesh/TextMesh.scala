package com.martomate.hexacraft.font.mesh

import scala.collection.mutable

class TextMesh(val vertexPositions: Seq[Float], val textureCoords: Seq[Float]) {
  def getVertexCount: Int = vertexPositions.length / 2
}

object TextMesh {
  val BaseLineHeight: Double = 0.03

  /** Calculates all the vertices for the quads on which the given lines of text will be rendered. */
  def fromLines(lines: Seq[Line], fontSize: Float, centered: Boolean, metaData: FontMetaData): TextMesh =
    var cursorX: Double = 0f
    var cursorY: Double = 0f

    val vertices: mutable.ArrayBuffer[Float] = new mutable.ArrayBuffer[Float]
    val textureCoords: mutable.ArrayBuffer[Float] = new mutable.ArrayBuffer[Float]

    for line <- lines do
      if centered then cursorX = (line.maxLength - line.currentLineLength) / 2

      for word <- line.getWords do
        for letter <- word.getCharacters do
          addQuad(
            vertices,
            cursorX + letter.xOffset * fontSize,
            -cursorY - letter.yOffset * fontSize,
            letter.sizeX * fontSize,
            -letter.sizeY * fontSize
          )
          addQuad(
            textureCoords,
            letter.textureBounds.x,
            letter.textureBounds.y,
            letter.textureBounds.w,
            letter.textureBounds.h
          )
          cursorX += letter.xAdvance * fontSize
        cursorX += metaData.getSpaceWidth * fontSize
      cursorX = 0
      cursorY += BaseLineHeight * fontSize

    new TextMesh(vertices.toSeq, textureCoords.toSeq)

  private def addQuad(vertices: mutable.ArrayBuffer[Float], x: Double, y: Double, w: Double, h: Double): Unit =
    val xLo: Float = x.toFloat
    val yLo: Float = y.toFloat
    val xHi: Float = (x + w).toFloat
    val yHi: Float = (y + h).toFloat

    // First triangle
    vertices += xLo
    vertices += yLo
    vertices += xLo
    vertices += yHi
    vertices += xHi
    vertices += yHi

    // Second triangle
    vertices += xHi
    vertices += yHi
    vertices += xHi
    vertices += yLo
    vertices += xLo
    vertices += yLo
}
