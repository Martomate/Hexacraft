package hexagon.gui.comp

import fontMeshCreator.GUIText
import hexagon.event.{CharEvent, KeyEvent, MouseClickEvent}
import org.joml.{Vector3f, Vector4f}
import org.lwjgl.glfw.GLFW

class TextField(_location: LocationInfo, initText: String = "", centered: Boolean = true, maxFontSize: Float = 2f) extends Component(_location) {
  private val bgColor = new Vector4f(0.5f)
  private val borderColor = new Vector4f(0.7f)// unused
  private val textColor = new Vector3f(1.0f)
  private var _text: String = _
  private var guiText: GUIText = _
  private var fontSize: Float = maxFontSize

  private var focused: Boolean = false

  private var cursorText = Component.makeText("|", location, maxFontSize)
  cursorText.setColour(textColor.x, textColor.y, textColor.z)
  private var cursorTextVisible: Boolean = false
  private var time: Int = 0

  setText(initText)

  def setText(newText: String): Unit = {
    val prevGuiText = guiText
    val prevFontSize = fontSize
    if (guiText != null) removeText(guiText)
    fontSize = maxFontSize
    guiText = Component.makeText(newText, location, fontSize, centered)
    guiText.setColour(textColor.x, textColor.y, textColor.z)
    try {
      addText(guiText)
      while (guiText.getNumberOfLines > 1) {
        removeText(guiText)
        fontSize *= 0.8f
        guiText = Component.makeText(newText, location, fontSize, centered)
        guiText.setColour(textColor.x, textColor.y, textColor.z)
        addText(guiText)
      }
      _text = newText
    } catch {
      case _: Exception =>
        println(s"$newText contains unsupported characters")
        guiText = prevGuiText
        fontSize = prevFontSize
        if (guiText != null) addText(guiText)
    }
    if (cursorTextVisible) removeText(cursorText)
    cursorText = Component.makeText("|", LocationInfo(location.x + location.w / 2f + guiText.getLineWidth(0).toFloat / 2f - fontSize * 0.002f, location.y + fontSize * 0.002f, location.h / 5, location.h), fontSize * 1.1f, false)
    cursorText.setColour(textColor.x, textColor.y, textColor.z)
    if (cursorTextVisible) addText(cursorText)
  }
  def text: String = _text

  def setBackgroundColor(r: Float, g: Float, b: Float, a: Float): Unit = bgColor.set(r, g, b, a)
  def setBorderColor(r: Float, g: Float, b: Float, a: Float): Unit = borderColor.set(r, g, b, a)
  def setTextColor(r: Float, g: Float, b: Float): Unit = textColor.set(r, g, b)

  override def tick(): Unit = {
    if (focused) {
      if (time % 30 == 0) {
        time = 0
        if (cursorTextVisible) {
          removeText(cursorText)
        } else {
          addText(cursorText)
        }
        cursorTextVisible = !cursorTextVisible
      }
      time += 1
    } else {
      time = 0
      if (cursorTextVisible) {
        removeText(cursorText)
        cursorTextVisible = false
      }
    }
  }

  override def render(transformation: GUITransformation): Unit = {
    Component.drawRect(location, transformation.x, transformation.y, bgColor)
    super.render(transformation)
  }

  override def onCharEvent(event: CharEvent): Boolean = {
    if (focused) {
      setText(_text + event.character.toChar)
      true
    } else false
  }

  override def onKeyEvent(event: KeyEvent): Boolean = {
    if (focused && event.action != GLFW.GLFW_RELEASE) {
      if (event.key == GLFW.GLFW_KEY_BACKSPACE) {
        if (_text.length > 0) setText(_text.substring(0, _text.length - 1))
      }
    }
    super.onKeyEvent(event)
  }

  override def onMouseClickEvent(event: MouseClickEvent): Boolean = {
    focused = location.containsPoint(event.mousePos)
    if (!focused) super.onMouseClickEvent(event)
    else true
  }
}
