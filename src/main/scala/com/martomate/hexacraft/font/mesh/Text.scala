package com.martomate.hexacraft.font.mesh

import com.martomate.hexacraft.font.TextMaster
import com.martomate.hexacraft.infra.gpu.OpenGL
import com.martomate.hexacraft.renderer.VAO

import org.joml.{Vector2f, Vector3f, Vector3fc}

class Text(
    var text: String,
    var fontSize: Float,
    val font: Font,
    val position: Vector2f,
    val maxLineLength: Float,
    val centered: Boolean
) {
  private var textMeshVao: VAO = _
  private var _vertexCount: Int = 0
  private val _color: Vector3f = new Vector3f(0f, 0f, 0f)
  private var numberOfLines: Int = 0
  private var lineWidths: Seq[Double] = Nil
  update()

  def vertexCount: Int = _vertexCount

  def setFontSize(fontSize: Float): Text =
    if this.fontSize != fontSize then
      this.fontSize = fontSize
      update()
    this

  def setText(text: String): Text =
    if this.text != text then
      this.text = text
      update()
    this

  def setTextAndFitSize(text: String, startSize: Float): Text =
    this.text = text
    this.fontSize = startSize
    update()
    while numberOfLines > 1 do // TODO: refactor so the loop variable is incremented here
      this.fontSize *= 0.8f
      update()
    this

  private def update(): Unit =
    val lines = WordWrapper(font.metaData, fontSize, maxLineLength).wrap(text)

    this.numberOfLines = lines.size
    this.lineWidths = lines.map(_.currentLineLength)

    val data: TextMesh = TextMesh.fromLines(lines, fontSize, centered, font.metaData)

    val vao: VAO = TextMaster.loadVAO(data.vertexPositions, data.textureCoords)
    setMeshInfo(vao, data.getVertexCount)

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
  def setColor(r: Float, g: Float, b: Float): Text =
    _color.set(r, g, b)
    this

  def getLineWidth(line: Int): Double = lineWidths(line)

  def setPosition(x: Float, y: Float): Text =
    position.x = x
    position.y = y
    this

  def getMesh: OpenGL.VertexArrayId = textMeshVao.id

  private def setMeshInfo(vao: VAO, verticesCount: Int): Unit =
    if textMeshVao ne vao then
      if textMeshVao != null then textMeshVao.free()
      this.textMeshVao = vao
    this._vertexCount = verticesCount

  def unload(): Unit = textMeshVao.free()
}
