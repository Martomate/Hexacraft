package hexacraft.text

import hexacraft.text.font.{Character, FontMetaData}
import hexacraft.text.layout.Line

import scala.collection.mutable

class TextMesh(val vertexPositions: Seq[Float], val textureCoords: Seq[Float]) {
  def getVertexCount: Int = vertexPositions.length / 2
}

object TextMesh {

  /** Calculates all the vertices for the quads on which the given lines of text will be rendered. */
  def fromLines(lines: Seq[Line], centered: Boolean, font: FontMetaData): TextMesh =
    val spaceWidth = font.spaceWidth
    val lineHeight = font.lineHeight

    var cursorX: Double = 0f
    var cursorY: Double = 0f

    val vertices: mutable.ArrayBuffer[Float] = new mutable.ArrayBuffer[Float]
    val textureCoords: mutable.ArrayBuffer[Float] = new mutable.ArrayBuffer[Float]

    for line <- lines do
      if centered then cursorX = (line.maxWidth - line.width) / 2

      for word <- line.words do
        for Character(_, tex, bounds) <- word.getCharacters do
          addQuad(vertices, cursorX + bounds.x, -cursorY - bounds.y, bounds.w, -bounds.h)
          addQuad(textureCoords, tex.x, tex.y, tex.w, tex.h)
          cursorX += bounds.xAdvance
        cursorX += spaceWidth
      cursorX = 0
      cursorY += lineHeight

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
