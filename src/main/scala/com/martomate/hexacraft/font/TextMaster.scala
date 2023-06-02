package com.martomate.hexacraft.font

import com.martomate.hexacraft.font.mesh.{FontType, GUIText}
import com.martomate.hexacraft.renderer.{VAO, VAOBuilder, VBOBuilder}

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
    new VAOBuilder(vertexPositions.length, 1)
      .addVBO(
        VBOBuilder()
          .floats(0, 2)
          .create(vertexPositions.length)
          .fillFloats(0, vertexPositions)
      )
      .addVBO(
        VBOBuilder().floats(1, 2).create(textureCoords.length).fillFloats(0, textureCoords)
      )
      .create()
  }
}
