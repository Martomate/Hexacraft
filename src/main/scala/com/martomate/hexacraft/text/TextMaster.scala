package com.martomate.hexacraft.text

import com.martomate.hexacraft.renderer.VAO
import com.martomate.hexacraft.text.font.Font

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class TextMaster {
  private val texts = mutable.HashMap.empty[Font, ArrayBuffer[Text]]
  private val renderer = new FontRenderer()

  def setWindowAspectRatio(aspectRatio: Float): Unit =
    renderer.setWindowAspectRatio(aspectRatio)

  def render(xoffset: Float, yoffset: Float): Unit = {
    renderer.render(texts, xoffset, yoffset)
  }

  def loadText(text: Text): Unit = {
    val font = text.font

    val textBatch = texts.getOrElseUpdate(font, new ArrayBuffer[Text])
    textBatch += text
  }

  def removeText(text: Text): Unit = {
    val textBatch = texts(text.font)
    textBatch -= text
    if (textBatch.isEmpty) {
      texts -= text.font
      // TODO: remove VAO
    }
  }

  def unload(): Unit = {
    for {
      font <- texts.keys
      text <- texts(font)
    } {
      text.unload()
    }
    texts.clear()
  }
}

object TextMaster {
  def loadVAO(vertexPositions: Seq[Float], textureCoords: Seq[Float]): VAO = {
    VAO
      .builder()
      .addVertexVbo(vertexPositions.length)(_.floats(0, 2), _.fillFloats(0, vertexPositions))
      .addVertexVbo(textureCoords.length)(_.floats(1, 2), _.fillFloats(0, textureCoords))
      .finish(vertexPositions.length)
  }
}
