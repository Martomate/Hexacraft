package com.martomate.hexacraft.font.mesh

import com.martomate.hexacraft.font.TextMaster
import com.martomate.hexacraft.renderer.VAO

import org.joml.{Vector2f, Vector3f, Vector3fc}

/** Represents a piece of text in the game.
  *
  * @author
  *   Karl
  */
class GUIText private (
    var textString: String,
    var fontSize: Float,
    val font: FontType,
    val position: Vector2f,
    val lineMaxSize: Float,
    val centerText: Boolean
) {
  private var textMeshVao: VAO = _
  private var _vertexCount: Int = 0
  private val _color: Vector3f = new Vector3f(0f, 0f, 0f)
  private var numberOfLines: Int = 0
  private var lineWidths: Seq[Double] = Nil
  update()

  def vertexCount: Int = _vertexCount

  def setFontSize(fontSize: Float): GUIText = {
    if (this.fontSize != fontSize) {
      this.fontSize = fontSize
      update()
    }
    this
  }

  def setText(text: String): GUIText = {
    if (this.textString != text) {
      this.textString = text
      update()
    }
    this
  }

  def setTextAndFitSize(text: String, startSize: Float): GUIText = {
    this.textString = text
    this.fontSize = startSize
    update()
    while (numberOfLines > 1) { // TODO: refactor so the loop variable is incremented here
      this.fontSize *= 0.8f
      update()
    }
    this
  }

  private def update(): Unit = {
    val data: TextMeshData = font.loadText(this)
    val vao: VAO = TextMaster.loadVAO(data.vertexPositions, data.textureCoords)
    setMeshInfo(vao, data.getVertexCount)
  }

  def color: Vector3f = _color

  /** Set the color of the text.
    *
    * @param r
    *   red value, between 0 and 1.
    * @param g
    *   green value, between 0 and 1.
    * @param b
    *   blue value, between 0 and 1.
    */
  def setColor(r: Float, g: Float, b: Float): GUIText = {
    _color.set(r, g, b)
    this
  }

  def getLineWidth(line: Int): Double = lineWidths(line)

  def setPosition(x: Float, y: Float): GUIText = {
    position.x = x
    position.y = y
    this
  }

  /** @return
    *   the ID of the text's VAO, which contains all the vertex data for the quads on which the text
    *   will be rendered.
    */
  def getMesh: Int = textMeshVao.id

  /** Set the VAO and vertex count for this text.
    *
    * @param vao
    *   the VAO containing all the vertex data for the quads on which the text will be rendered.
    * @param verticesCount
    *   the total number of vertices in all of the quads.
    */
  def setMeshInfo(vao: VAO, verticesCount: Int): Unit = {
    if (textMeshVao ne vao) {
      if (textMeshVao != null) textMeshVao.free()
      this.textMeshVao = vao
    }
    this._vertexCount = verticesCount
  }

  /** Sets the number of lines that this text covers (method used only in loading). */
  def setNumberOfLines(number: Int): Unit = {
    this.numberOfLines = number
  }

  def isCentered: Boolean = centerText

  def setLineWidths(lineWidths: Seq[Double]): Unit = {
    this.lineWidths = lineWidths
  }

  def unload(): Unit = {
    textMeshVao.free()
  }
}

object GUIText {

  /** Creates a new text, loads the text's quads into a VAO, and adds the text to the screen.
    *
    * @param text
    *   the text.
    * @param fontSize
    *   the font size of the text, where a font size of 1 is the default size.
    * @param font
    *   the font that this text should use.
    * @param position
    *   the position on the screen where the top left corner of the text should be rendered. The top
    *   left corner of the screen is (0, 0) and the bottom right is (1, 1).
    * @param maxLineLength
    *   basically the width of the virtual page in terms of screen width (1 is full screen width,
    *   0.5 is half the width of the screen, etc.) Text cannot go off the edge of the page, so if
    *   the text is longer than this length it will go onto the next line. When text is centered it
    *   is centered into the middle of the line, based on this line length value.
    * @param centered
    *   whether the text should be centered or not.
    */
  def apply(
      text: String,
      fontSize: Float,
      font: FontType,
      position: Vector2f,
      maxLineLength: Float,
      centered: Boolean
  ): GUIText = new GUIText(
    text,
    fontSize,
    font,
    position,
    maxLineLength,
    centered
  )
}
