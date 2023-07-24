package com.martomate.hexacraft.font.mesh

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object TextMeshBuilder {
  val BaseLineHeight: Double = 0.03

  private def addRectangle(vertices: mutable.ArrayBuffer[Float], x: Double, y: Double, w: Double, h: Double): Unit =
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

class TextMeshBuilder(val metaData: FontMetaData) {
  def createTextMesh(text: GUIText): TextMeshData =
    val lines = createLines(text.textString, text.fontSize, text.lineMaxSize)
    text.setNumberOfLines(lines.size)
    text.setLineWidths(lines.map(_.currentLineLength))
    createQuadVertices(lines, text.fontSize, text.isCentered)

  private def createLines(text: String, fontSize: Float, maxLineLength: Float): Seq[Line] =
    val lines = new mutable.ArrayBuffer[Line]

    var currentLine: Line = new Line(metaData.getSpaceWidth, fontSize, maxLineLength)

    val words: Array[Word] =
      for s <- text.split(' ')
      yield
        val w = new Word(fontSize)
        for c <- s.toCharArray do
          val character: Character = metaData.getCharacter(c.toInt)
          w.addCharacter(character)
        w

    for w <- words do
      val added: Boolean = currentLine.attemptToAddWord(w)
      if !added then
        lines += currentLine
        currentLine = new Line(metaData.getSpaceWidth, fontSize, maxLineLength)
        val couldFit = currentLine.attemptToAddWord(w)

        // The following is a workaround for auto-resizing texts to detect an overflow
        if !couldFit then lines += new Line(metaData.getSpaceWidth, fontSize, maxLineLength)

    if currentLine.getWords.nonEmpty then lines += currentLine

    lines.toSeq

  private def createQuadVertices(lines: Seq[Line], fontSize: Float, centered: Boolean): TextMeshData =
    var cursorX: Double = 0f
    var cursorY: Double = 0f

    val vertices: mutable.ArrayBuffer[Float] = new mutable.ArrayBuffer[Float]
    val textureCoords: mutable.ArrayBuffer[Float] = new mutable.ArrayBuffer[Float]

    for line <- lines do
      if centered then cursorX = (line.maxLength - line.currentLineLength) / 2

      for word <- line.getWords do
        for letter <- word.getCharacters do
          TextMeshBuilder.addRectangle(
            vertices,
            cursorX + letter.xOffset * fontSize,
            -cursorY - letter.yOffset * fontSize,
            letter.sizeX * fontSize,
            -letter.sizeY * fontSize
          )
          TextMeshBuilder.addRectangle(
            textureCoords,
            letter.textureBounds.x,
            letter.textureBounds.y,
            letter.textureBounds.w,
            letter.textureBounds.h
          )
          cursorX += letter.xAdvance * fontSize
        cursorX += metaData.getSpaceWidth * fontSize
      cursorX = 0
      cursorY += TextMeshBuilder.BaseLineHeight * fontSize

    new TextMeshData(vertices.toSeq, textureCoords.toSeq)

}
