package hexagon.gui.comp

import fontMeshCreator.GUIText
import org.joml.{Vector3f, Vector4f}
import org.lwjgl.glfw.GLFW

class TextField(_location: LocationInfo, initText: String = "") extends Component(_location) {
  private val bgColor = new Vector4f(0.5f)
  private val borderColor = new Vector4f(0.7f)
  private val textColor = new Vector3f(1.0f)
  private var _text: String = _
  private var guiText: GUIText = _
  setText(initText)

  def setText(newText: String): Unit = {
    val prevGuiText = guiText
    if (guiText != null) removeText(guiText)
    guiText = Component.makeText(newText, location, 2)
    guiText.setColour(textColor.x, textColor.y, textColor.z)
    try {
      addText(guiText)
      _text = newText
    } catch {
      case _: Exception =>
        println(s"$newText contains unsupported characters")
        guiText = prevGuiText
        if (guiText != null) addText(guiText)
    }
  }
  def text: String = _text

  def setBackgroundColor(r: Float, g: Float, b: Float, a: Float): Unit = bgColor.set(r, g, b, a)
  def setBorderColor(r: Float, g: Float, b: Float, a: Float): Unit = borderColor.set(r, g, b, a)
  def setTextColor(r: Float, g: Float, b: Float): Unit = textColor.set(r, g, b)

  override def render(): Unit = {
    Component.drawRect(location, bgColor)
    super.render()
  }

  override def onCharEvent(character: Int): Unit = {
    setText(_text + character.toChar)
  }

  override def onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Unit = {
    if (action != GLFW.GLFW_RELEASE) {
      if (key == GLFW.GLFW_KEY_BACKSPACE) {
        if (_text.length > 0) setText(_text.substring(0, _text.length - 1))
      }
    }
    super.onKeyEvent(key, scancode, action, mods)
  }
}
