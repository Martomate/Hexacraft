package hexacraft.text

import hexacraft.text.font.Font

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class TextMaster {
  private val texts = mutable.HashMap.empty[Font, ArrayBuffer[Text]]
  private val renderer = new FontRenderer()

  def setWindowAspectRatio(aspectRatio: Float): Unit = {
    renderer.setWindowAspectRatio(aspectRatio)
  }

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
    if textBatch.isEmpty then {
      texts -= text.font
      // TODO: remove VAO
    }
  }

  def unload(): Unit = {
    for {
      font <- texts.keys
      text <- texts(font)
    } do {
      text.unload()
    }
    texts.clear()
  }
}
