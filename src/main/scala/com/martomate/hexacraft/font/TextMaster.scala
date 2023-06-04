package com.martomate.hexacraft.font

import com.martomate.hexacraft.font.mesh.{FontType, GUIText}
import com.martomate.hexacraft.renderer.VAO

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class TextMaster {
  private val texts = mutable.HashMap.empty[FontType, ArrayBuffer[GUIText]]
  private val renderer = new FontRenderer()

  def render(xoffset: Float, yoffset: Float): Unit = {
    renderer.render(texts, xoffset, yoffset)
  }

  def loadText(text: GUIText): Unit = {
    val font = text.font

    val textBatch = texts.getOrElseUpdate(font, new ArrayBuffer[GUIText])
    textBatch += text
  }

  def removeText(text: GUIText): Unit = {
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
  def loadVAO(vertexPositions: Array[Float], textureCoords: Array[Float]): VAO = {
    VAO
      .builder()
      .addVertexVbo(vertexPositions.length)(_.floats(0, 2), _.fillFloats(0, vertexPositions))
      .addVertexVbo(textureCoords.length)(_.floats(1, 2), _.fillFloats(0, textureCoords))
      .finish(vertexPositions.length)
  }
}
