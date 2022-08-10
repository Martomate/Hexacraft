package com.martomate.hexacraft.font.mesh

import java.net.URL
import scala.collection.mutable

object TextMeshCreator {
  val LINE_HEIGHT: Double = 0.03f
  val SPACE_ASCII: Int = 32

  private def addVertices(vertices: mutable.ArrayBuffer[Float], x: Double, y: Double, maxX: Double, maxY: Double): Unit = {
    vertices += x.toFloat
    vertices += y.toFloat
    vertices += x.toFloat
    vertices += maxY.toFloat
    vertices += maxX.toFloat
    vertices += maxY.toFloat
    vertices += maxX.toFloat
    vertices += maxY.toFloat
    vertices += maxX.toFloat
    vertices += y.toFloat
    vertices += x.toFloat
    vertices += y.toFloat
  }

  private def addTexCoords(texCoords: mutable.ArrayBuffer[Float], x: Double, y: Double, maxX: Double, maxY: Double): Unit = {
    texCoords += x.toFloat
    texCoords += y.toFloat
    texCoords += x.toFloat
    texCoords += maxY.toFloat
    texCoords += maxX.toFloat
    texCoords += maxY.toFloat
    texCoords += maxX.toFloat
    texCoords += maxY.toFloat
    texCoords += maxX.toFloat
    texCoords += y.toFloat
    texCoords += x.toFloat
    texCoords += y.toFloat
  }

  private def listToArray(listOfFloats: mutable.ArrayBuffer[Float]): Array[Float] = {
    val array: Array[Float] = new Array[Float](listOfFloats.size)
    for (i <- array.indices) {
      array(i) = listOfFloats(i)
    }
    array
  }
}

class TextMeshCreator (val metaFile: URL) {
  private var metaData: MetaFile = new MetaFile(metaFile)

  def createTextMesh(text: GUIText): TextMeshData = {
    val lines = createStructure(text)
    createQuadVertices(text, lines)
  }

  private def createStructure(text: GUIText): Seq[Line] = {
    val chars: Array[Char] = text.getTextString.toCharArray
    val lines = new mutable.ArrayBuffer[Line]
    var currentLine: Line = new Line(metaData.getSpaceWidth, text.getFontSize, text.getMaxLineSize)
    var currentWord: Word = new Word(text.getFontSize)
    for (c <- chars) {
      val ascii: Int = c.toInt
      if (ascii == TextMeshCreator.SPACE_ASCII) {
        val added: Boolean = currentLine.attemptToAddWord(currentWord)
        if (!added) {
          lines += currentLine
          currentLine = new Line(metaData.getSpaceWidth, text.getFontSize, text.getMaxLineSize)
          currentLine.attemptToAddWord(currentWord)
        }
        currentWord = new Word(text.getFontSize)
      } else {
        val character: Character = metaData.getCharacter(ascii)
        currentWord.addCharacter(character)
      }
    }
    completeStructure(lines, currentLine, currentWord, text)
    lines.toSeq
  }

  private def completeStructure(lines: mutable.ArrayBuffer[Line], currentLine: Line, currentWord: Word, text: GUIText): Unit = {
    val added: Boolean = currentLine.attemptToAddWord(currentWord)
    lines += currentLine
    if (!added) {
      val newLine = new Line(metaData.getSpaceWidth, text.getFontSize, text.getMaxLineSize)
      newLine.attemptToAddWord(currentWord)
      lines += newLine
    }
  }

  private def createQuadVertices(text: GUIText, lines: Seq[Line]): TextMeshData = {
    text.setNumberOfLines(lines.size)
    text.setLineWidths(lines.map(_.getLineLength))
    var curserX: Double = 0f
    var curserY: Double = 0f
    val vertices: mutable.ArrayBuffer[Float] = new mutable.ArrayBuffer[Float]
    val textureCoords: mutable.ArrayBuffer[Float] = new mutable.ArrayBuffer[Float]
    for (line <- lines) {
      if (text.isCentered) curserX = (line.getMaxLength - line.getLineLength) / 2
      for (word <- line.getWords) {
        for (letter <- word.getCharacters) {
          addVerticesForCharacter(curserX, curserY, letter, text.getFontSize, vertices)
          TextMeshCreator.addTexCoords(textureCoords, letter.getxTextureCoord, letter.getyTextureCoord, letter.getXMaxTextureCoord, letter.getYMaxTextureCoord)
          curserX += letter.getxAdvance * text.getFontSize
        }
        curserX += metaData.getSpaceWidth * text.getFontSize
      }
      curserX = 0
      curserY += TextMeshCreator.LINE_HEIGHT * text.getFontSize
    }
    new TextMeshData(TextMeshCreator.listToArray(vertices), TextMeshCreator.listToArray(textureCoords))
  }

  private def addVerticesForCharacter(curserX: Double, curserY: Double, character: Character, fontSize: Double, vertices: mutable.ArrayBuffer[Float]): Unit = {
    val x: Double = curserX + (character.getxOffset * fontSize)
    val y: Double = curserY + (character.getyOffset * fontSize)
    val maxX: Double = x + (character.getSizeX * fontSize)
    val maxY: Double = y + (character.getSizeY * fontSize)
    val properX: Double = x
    val properY: Double = -y
    val properMaxX: Double = maxX
    val properMaxY: Double = -maxY
    TextMeshCreator.addVertices(vertices, properX, properY, properMaxX, properMaxY)
  }
}
