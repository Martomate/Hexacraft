package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.gui.{CharEvent, KeyEvent, MouseClickEvent}
import com.martomate.hexacraft.gui.location.{LocationInfo, LocationInfoIdentity}
import fontMeshCreator.GUIText
import org.joml.{Vector3f, Vector4f}
import org.lwjgl.glfw.GLFW

class TextField(_location: LocationInfo, initText: String = "", centered: Boolean = true, maxFontSize: Float = 4f)(implicit window: GameWindow) extends Component(_location) {
  private val bgColor = new Vector4f(0.5f)
  private val textColor = new Vector3f(1.0f)
  private val guiText: GUIText = Component.makeText(initText, location, maxFontSize, centered).setColour(textColor.x, textColor.y, textColor.z)
  private val cursorText = Component.makeText("|", new LocationInfoIdentity(location.x + location.w / 2f - maxFontSize * 0.002f, location.y + maxFontSize * 0.002f, location.h / 4, location.h), maxFontSize * 1.1f, centered = false).setColour(textColor.x, textColor.y, textColor.z)
  private var cursorTextVisible: Boolean = false
  private var time: Int = 0

  private var focused: Boolean = false

  addText(guiText)
  setText(initText)

  def setText(newText: String): Unit = {
    val prevText = text
    val prevFontSize = guiText.getFontSize
    try {
      guiText.setTextAndFitSize(newText, maxFontSize)
    } catch {
      case _: Exception =>
        println(s"$newText contains unsupported characters")
        guiText.setTextAndFitSize(prevText, prevFontSize)
    }
    if (cursorTextVisible) removeText(cursorText)
    val fontSize = guiText.getFontSize
    cursorText.setPosition(location.x + location.w / 2f + guiText.getLineWidth(0).toFloat / 2f - fontSize * 0.002f, cursorText.getPosition.y)
    cursorText.setFontSize(fontSize * 1.1f)
    if (cursorTextVisible) addText(cursorText)
  }
  def text: String = guiText.getText

  def setBackgroundColor(r: Float, g: Float, b: Float, a: Float): Unit = bgColor.set(r, g, b, a)
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
      setText(text + event.character.toChar)
      true
    } else false
  }

  override def onKeyEvent(event: KeyEvent): Boolean = {
    if (focused && event.action != GLFW.GLFW_RELEASE) {
      if (event.key == GLFW.GLFW_KEY_BACKSPACE) {
        if (text.nonEmpty) setText(text.substring(0, text.length - 1))
      }
    }
    super.onKeyEvent(event)
  }

  override def onMouseClickEvent(event: MouseClickEvent): Boolean = {
    focused = location.containsPoint(event.mousePos)
    if (!focused) super.onMouseClickEvent(event)
    else false
  }
}
