package hexacraft.gui.comp

import hexacraft.gui.{Event, LocationInfo, RenderContext, TickContext}
import hexacraft.infra.window.{KeyAction, KeyboardKey}
import hexacraft.text.Text

import org.joml.{Vector3f, Vector4f}

class TextField(
    location: LocationInfo,
    initText: String = "",
    centered: Boolean = true,
    maxFontSize: Float = 4f,
    alwaysFocused: Boolean = false,
    backgroundEnabled: Boolean = true
) extends Component {
  private val bgColor = new Vector4f(0.4f, 0.4f, 0.4f, 0.8f)
  private val textColor = new Vector3f(1.0f)

  private val contentText: Text = makeContentText()
  private val cursorText: Text = makeCursorText()

  private var cursorTextVisible: Boolean = false
  private var time: Int = 0
  private var focused: Boolean = alwaysFocused

  addText(contentText)
  setText(initText)

  def setText(newText: String): Unit = {
    val prevText = text
    val prevFontSize = contentText.fontSize

    try {
      contentText.setTextAndFitSize(newText, maxFontSize)
    } catch {
      case _: Exception =>
        println(s"$newText contains unsupported characters")
        contentText.setTextAndFitSize(prevText, prevFontSize)
    }

    if cursorTextVisible then {
      removeText(cursorText)
    }

    val fontSize = contentText.fontSize
    val newCursorTextX =
      if centered then {
        location.x + location.w / 2f + contentText.getLineWidth(0).toFloat / 2f - fontSize * 0.002f
      } else {
        location.x + contentText.getLineWidth(0).toFloat - fontSize * 0.002f
      }
    val newCursorTextY = cursorText.position.y

    cursorText.setPosition(newCursorTextX, newCursorTextY)
    cursorText.setFontSize(fontSize * 1.1f)

    if cursorTextVisible then {
      addText(cursorText)
    }
  }

  def text: String = contentText.text

  override def tick(ctx: TickContext): Unit = {
    if focused then {
      if time % 30 == 0 then {
        time = 0

        if cursorTextVisible then {
          removeText(cursorText)
        } else {
          addText(cursorText)
        }

        cursorTextVisible = !cursorTextVisible
      }
      time += 1
    } else {
      time = 0
      if cursorTextVisible then {
        removeText(cursorText)
        cursorTextVisible = false
      }
    }
  }

  override def render(context: RenderContext): Unit = {
    if this.backgroundEnabled then {
      Component.drawFancyRect(
        location,
        context.offset.x,
        context.offset.y,
        bgColor,
        context.windowAspectRatio,
        inverted = true
      )
    }
    super.render(context)
  }

  override def handleEvent(event: Event): Boolean = {
    import Event.*
    var captureEvent = false
    event match {
      case CharEvent(character) if focused =>
        setText(text + character.toChar)
        captureEvent = true
      case KeyEvent(KeyboardKey.Backspace, _, KeyAction.Press, _) =>
        if focused && text.nonEmpty then {
          setText(text.substring(0, text.length - 1))
        }
      case MouseClickEvent(_, _, _, mousePos) =>
        focused = alwaysFocused || location.containsPoint(mousePos)
      case _ =>
    }
    captureEvent
  }

  private def makeContentText() = {
    Component
      .makeText(initText, location, maxFontSize, centered)
      .setColor(textColor.x, textColor.y, textColor.z)
  }

  private def makeCursorText() = {
    val textLocation = LocationInfo(
      location.x + (if centered then location.w / 2f else 0f) - maxFontSize * 0.002f,
      location.y + maxFontSize * 0.002f,
      location.h / 4,
      location.h
    )

    Component
      .makeText("|", textLocation, maxFontSize * 1.1f, centered = false)
      .setColor(textColor.x, textColor.y, textColor.z)
  }
}
