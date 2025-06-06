package hexacraft.client

import hexacraft.game.ServerMessage
import hexacraft.game.ServerMessage.Sender
import hexacraft.gui.{Event, LocationInfo, RenderContext}
import hexacraft.gui.comp.{Component, SubComponents, TextField}
import hexacraft.infra.window.{KeyAction, KeyboardKey}
import hexacraft.text.Text
import hexacraft.util.Channel

import org.joml.{Vector3f, Vector4f}

import scala.collection.mutable

object ChatOverlay {
  enum Event {
    case MessageSubmitted(message: String)
    case Closed
  }
}

class ChatOverlay(eventHandler: Channel.Sender[ChatOverlay.Event]) extends Component with SubComponents {
  private val texts = mutable.ArrayBuffer.empty[Text]

  private var input: Option[TextField] = None

  def isInputEnabled: Boolean = input.isDefined

  def addMessage(m: ServerMessage): Unit = {
    // TODO: include player name somehow

    for t <- texts do {
      t.position.y += 0.06f
    }

    val prefix = m.sender match {
      case Sender.Server          => ""
      case Sender.Player(_, name) => s"$name: "
    }

    val color = m.sender match {
      case Sender.Server       => Vector3f(0.9f, 0.9f, 0.2f)
      case Sender.Player(_, _) => Vector3f(0.9f, 0.9f, 0.9f)
    }

    val bold = m.sender match {
      case Sender.Server       => true
      case Sender.Player(_, _) => false
    }

    val location = LocationInfo.from16x9(0.01f, 0.20f, 0.3f, 0.05f)
    val guiText = Component.makeText(
      prefix + m.text,
      location,
      2,
      centered = false,
      shadow = true,
      bold = bold,
      color = color
    )
    this.addText(guiText)
    texts += guiText
  }

  def setInputEnabled(enabled: Boolean): Unit = {
    if enabled then {
      if input.isEmpty then {
        val c = TextField(
          LocationInfo.from16x9(0.01f, 0.15f, 0.20f, 0.05f),
          "",
          maxFontSize = 2,
          alwaysFocused = true,
          centered = false,
          backgroundEnabled = false
        )
        input = Some(c)

        this.addComponent(c)
      }
    } else {
      if input.isDefined then {
        val c = input.get
        input = None

        this.removeComponent(c)
        c.unload()
      }
    }
  }

  override def handleEvent(event: Event): Boolean = {
    if input.isDefined then {
      event match {
        case Event.KeyEvent(KeyboardKey.Escape, _, KeyAction.Press, _) =>
          eventHandler.send(ChatOverlay.Event.Closed)
        case Event.KeyEvent(KeyboardKey.Enter, _, KeyAction.Press, _) =>
          val message = input.get.text
          input.get.setText("")
          eventHandler.send(ChatOverlay.Event.MessageSubmitted(message))
        case e =>
          super.handleEvent(event)
      }
      true
    } else {
      super.handleEvent(event)
    }
  }

  override def render(context: RenderContext): Unit = {
    texts.foreach(t => t.setPosition(-context.windowAspectRatio + 0.01f * 2 * 16 / 9, t.position.y))
    val location = LocationInfo.from16x9(0.0f, 0.20f, 0.3f, 0.02f + 0.03f * texts.size)
    Component.drawRect(location, context.offset.x, context.offset.y, Vector4f(0, 0, 0, 0.3f), context.windowAspectRatio)
    super.render(context)
  }
}
