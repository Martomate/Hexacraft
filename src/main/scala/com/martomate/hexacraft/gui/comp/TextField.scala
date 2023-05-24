package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.font.mesh.GUIText
import com.martomate.hexacraft.gui.{Event, LocationInfo}

import org.joml.{Vector3f, Vector4f}
import org.lwjgl.glfw.GLFW

class TextField(
    location: LocationInfo,
    initText: String = "",
    centered: Boolean = true,
    maxFontSize: Float = 4f
) extends Component:
  private val bgColor = new Vector4f(0.5f)
  private val textColor = new Vector3f(1.0f)

  private val contentText: GUIText = makeContentText()
  private val cursorText: GUIText = makeCursorText()

  private var cursorTextVisible: Boolean = false
  private var time: Int = 0
  private var focused: Boolean = false

  addText(contentText)
  setText(initText)

  def setText(newText: String): Unit =
    val prevText = text
    val prevFontSize = contentText.fontSize

    try contentText.setTextAndFitSize(newText, maxFontSize)
    catch
      case _: Exception =>
        println(s"$newText contains unsupported characters")
        contentText.setTextAndFitSize(prevText, prevFontSize)

    if cursorTextVisible
    then removeText(cursorText)

    val fontSize = contentText.fontSize
    val newCursorTextX =
      location.x + location.w / 2f + contentText.getLineWidth(0).toFloat / 2f - fontSize * 0.002f
    val newCursorTextY = cursorText.position.y

    cursorText.setPosition(newCursorTextX, newCursorTextY)
    cursorText.setFontSize(fontSize * 1.1f)

    if (cursorTextVisible) addText(cursorText)

  def text: String = contentText.textString

  override def tick(): Unit =
    if focused
    then
      if time % 30 == 0
      then
        time = 0

        if cursorTextVisible
        then removeText(cursorText)
        else addText(cursorText)

        cursorTextVisible = !cursorTextVisible
      time += 1
    else
      time = 0
      if cursorTextVisible
      then
        removeText(cursorText)
        cursorTextVisible = false

  override def render(transformation: GUITransformation): Unit =
    Component.drawRect(location, transformation.x, transformation.y, bgColor)
    super.render(transformation)

  override def handleEvent(event: Event): Boolean = event match
    case Event.CharEvent(character) =>
      if focused
      then
        setText(text + character.toChar)
        true
      else false
    case Event.KeyEvent(key, _, action, _) =>
      val keyReleased = action != Event.KeyAction.Release

      if focused && keyReleased && key == GLFW.GLFW_KEY_BACKSPACE && text.nonEmpty
      then setText(text.substring(0, text.length - 1))

      super.handleEvent(event)
    case Event.MouseClickEvent(_, _, _, mousePos) =>
      focused = location.containsPoint(mousePos)
      if !focused
      then super.handleEvent(event)
      else false
    case _ => super.handleEvent(event)

  private def makeContentText() =
    Component
      .makeText(initText, location, maxFontSize, centered)
      .setColor(textColor.x, textColor.y, textColor.z)

  private def makeCursorText() =
    val textLocation = LocationInfo(
      location.x + location.w / 2f - maxFontSize * 0.002f,
      location.y + maxFontSize * 0.002f,
      location.h / 4,
      location.h
    )

    Component
      .makeText("|", textLocation, maxFontSize * 1.1f, centered = false)
      .setColor(textColor.x, textColor.y, textColor.z)
