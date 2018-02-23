package hexagon.font

import fontMeshCreator.{FontType, GUIText}
import hexagon.renderer.{VAO, VAOBuilder, VBO}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class TextMaster {
  private val texts = mutable.HashMap.empty[FontType, ArrayBuffer[GUIText]]
  private val renderer = new FontRenderer()

  def render(xoffset: Float, yoffset: Float): Unit = {
    renderer.render(texts, xoffset, yoffset)
  }

  def loadText(text: GUIText): Unit = {
    val font = text.getFont

    var textBatch = texts.getOrElseUpdate(font, new ArrayBuffer[GUIText])
    textBatch += text
  }

  def removeText(text: GUIText): Unit = {
    val textBatch = texts(text.getFont)
    textBatch -= text
    if (textBatch.isEmpty) {
      texts -= text.getFont
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
  def loadVAO(vertexPositions: Array[Float], textureCoords: Array[Float]): VAO = {
    new VAOBuilder(vertexPositions.length, 1)
    .addVBO(VBO.apply(vertexPositions.length).floats(0, 2).create().fillFloats(0, vertexPositions))
    .addVBO(VBO.apply(textureCoords.length).floats(1, 2).create().fillFloats(0, textureCoords))
    .create()
  }
}