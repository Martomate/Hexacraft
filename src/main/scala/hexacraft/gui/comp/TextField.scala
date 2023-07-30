package hexacraft.gui.comp

import hexacraft.gui.{Event, LocationInfo, RenderContext}
import hexacraft.infra.window.{KeyAction, KeyboardKey}
import hexacraft.text.Text

import org.joml.{Vector3f, Vector4f}

class TextField(
    location: LocationInfo,
    initText: String = "",
    centered: Boolean = true,
    maxFontSize: Float = 4f
) extends Component:
  private val bgColor = new Vector4f(0.5f)
  private val textColor = new Vector3f(1.0f)

  private val contentText: Text = makeContentText()
  private val cursorText: Text = makeCursorText()

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

  def text: String = contentText.text

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

  override def render(transformation: GUITransformation)(using context: RenderContext): Unit =
    Component.drawRect(location, transformation.x, transformation.y, bgColor, context.windowAspectRatio)
    super.render(transformation)

  override def handleEvent(event: Event): Boolean =
    import Event.*
    var captureEvent = false
    event match
      case CharEvent(character) if focused =>
        setText(text + character.toChar)
        captureEvent = true
      case KeyEvent(KeyboardKey.Backspace, _, KeyAction.Press, _) =>
        if focused && text.nonEmpty
        then setText(text.substring(0, text.length - 1))
      case MouseClickEvent(_, _, _, mousePos) =>
        focused = location.containsPoint(mousePos)
      case _ =>
    captureEvent

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
