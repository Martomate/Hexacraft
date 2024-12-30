package hexacraft.text

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.VAO
import hexacraft.shaders.FontShader
import hexacraft.text.font.Font
import hexacraft.text.layout.LineBreaker

import org.joml.{Vector2f, Vector3f}

object Text {
  private val BaseLineHeight: Double = 0.03
}

class Text(
    var text: String,
    var fontSize: Float,
    val font: Font,
    val position: Vector2f,
    val maxLineLength: Float,
    val centered: Boolean,
    val hasShadow: Boolean
) {
  private var textMeshVao: VAO = null.asInstanceOf[VAO]
  private var _vertexCount: Int = 0
  private val _color: Vector3f = new Vector3f(0f, 0f, 0f)
  private val _shadowColor: Vector3f = new Vector3f(0f, 0f, 0f)
  private var numberOfLines: Int = 0
  private var lineWidths: Seq[Double] = Nil
  update()

  def vertexCount: Int = _vertexCount

  def setFontSize(fontSize: Float): Text = {
    if this.fontSize != fontSize then {
      this.fontSize = fontSize
      update()
    }
    this
  }

  def setText(text: String): Text = {
    if this.text != text then {
      this.text = text
      update()
    }
    this
  }

  def setTextAndFitSize(text: String, startSize: Float): Text = {
    this.text = text
    this.fontSize = startSize
    update()
    while numberOfLines > 1 do { // TODO: refactor so the loop variable is incremented here
      this.fontSize *= 0.8f
      update()
    }
    this
  }

  private def update(): Unit = {
    val metaData = font.getMetaData(Text.BaseLineHeight * fontSize)

    val lines = LineBreaker(maxLineLength).layout(text, metaData)

    this.numberOfLines = lines.size
    this.lineWidths = lines.map(_.width)

    val data: TextMesh = TextMesh.fromLines(lines, centered, metaData)

    val vao: VAO = FontShader.createVao(data.vertexPositions, data.textureCoords)
    setMeshInfo(vao, data.getVertexCount)
  }

  def color: Vector3f = _color
  def shadowColor: Vector3f = _shadowColor

  /** Set the color of the text.
    *
    * @param r
    *   red value, between 0 and 1.
    * @param g
    *   green value, between 0 and 1.
    * @param b
    *   blue value, between 0 and 1.
    */
  def setColor(r: Float, g: Float, b: Float): Text = {
    _color.set(r, g, b)
    this
  }

  def setShadowColor(r: Float, g: Float, b: Float): Text = {
    _shadowColor.set(r, g, b)
    this
  }

  def getLineWidth(line: Int): Double = {
    if lineWidths.isEmpty then 0 else lineWidths(line)
  }

  def setPosition(x: Float, y: Float): Text = {
    position.x = x
    position.y = y
    this
  }

  def offsetPosition(dx: Float, dy: Float): Text = {
    setPosition(position.x + dx, position.y + dy)
  }

  def getMesh: OpenGL.VertexArrayId = textMeshVao.id

  private def setMeshInfo(vao: VAO, verticesCount: Int): Unit = {
    if textMeshVao ne vao then {
      if textMeshVao != null then {
        textMeshVao.free()
      }
      this.textMeshVao = vao
    }
    this._vertexCount = verticesCount
  }

  def unload(): Unit = {
    textMeshVao.free()
  }
}
