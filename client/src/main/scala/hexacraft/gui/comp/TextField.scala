package hexacraft.gui.comp

import hexacraft.gui.{Event, LocationInfo, RenderContext, TickContext}
import hexacraft.infra.os.{Mac, OS}
import hexacraft.infra.window.{KeyAction, KeyboardKey, KeyMods}
import hexacraft.text.Text

import org.joml.{Vector3f, Vector4f}

class TextField(
    location: LocationInfo,
    initText: String = "",
    centered: Boolean = true,
    maxFontSize: Float = 4f,
    alwaysFocused: Boolean = false,
    backgroundEnabled: Boolean = true,
    fancyBackground: Boolean = true,
    bgColor: Vector4f = new Vector4f(0.4f, 0.4f, 0.4f, 0.8f),
    padding: Float = 0.005f
) extends Component {
  private val textColor = new Vector3f(1.0f)

  private val contentText: Text = makeContentText()
  private val cursorText: Text = makeCursorText()

  private var cursorTextVisible: Boolean = false
  private var time: Int = 0
  private var focused: Boolean = alwaysFocused
  private var shouldPasteFromClipboard: Boolean = false

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
        location.x + padding + contentText.getLineWidth(0).toFloat - fontSize * 0.002f
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

    if shouldPasteFromClipboard then {
      shouldPasteFromClipboard = false
      setText(text + ctx.clipboard)
    }
  }

  override def render(context: RenderContext): Unit = {
    if this.backgroundEnabled then {
      if this.fancyBackground then {
        Component.drawFancyRect(
          location,
          context.offset.x,
          context.offset.y,
          bgColor,
          context.windowAspectRatio,
          inverted = true
        )
      } else {
        Component.drawRect(
          location,
          context.offset.x,
          context.offset.y,
          bgColor,
          context.windowAspectRatio
        )
      }
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
      case KeyEvent(KeyboardKey.Letter('V'), _, KeyAction.Release, mods) =>
        if focused then {
          val isAttemptingPaste = if OS.current == Mac then mods.superDown else mods.ctrlDown
          if isAttemptingPaste then {
            shouldPasteFromClipboard = true
          }
        }
      case MouseClickEvent(_, _, _, mousePos) =>
        focused = alwaysFocused || location.containsPoint(mousePos)
      case _ =>
    }
    captureEvent
  }

  private def makeContentText() = {
    Component
      .makeText(initText, location.expand(-padding), maxFontSize, centered)
      .setColor(textColor.x, textColor.y, textColor.z)
  }

  private def makeCursorText() = {
    val textLocation = LocationInfo(
      location.x + (if centered then location.w / 2f else padding) - maxFontSize * 0.002f,
      location.y + maxFontSize * 0.002f,
      location.h / 3,
      location.h
    )

    Component
      .makeText("|", textLocation, maxFontSize * 1.1f, centered = false)
      .setColor(textColor.x, textColor.y, textColor.z)
  }
}
