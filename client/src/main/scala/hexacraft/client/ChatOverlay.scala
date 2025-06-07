package hexacraft.client

import hexacraft.game.ServerMessage
import hexacraft.game.ServerMessage.Sender
import hexacraft.gui.{Event, LocationInfo, RenderContext}
import hexacraft.gui.comp.{Component, Label, ScrollPane, SubComponents, TextField}
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
  private val texts = mutable.ArrayBuffer.empty[(Long, ServerMessage)]

  private val scrollPaneVerticalPadding = 0.005f
  private val scrollPaneBounds = LocationInfo.from16x9(0.005f, 0.20f, 0.3f, scrollPaneVerticalPadding + 0.03f * 16)
  private var scrollPane: Option[ScrollPane] = Some(makeScrollPane(active = false))
  addComponent(scrollPane.get)

  private var input: Option[TextField] = None

  def isInputEnabled: Boolean = input.isDefined

  private def makeScrollPane(active: Boolean): ScrollPane = {
    val pane = ScrollPane(
      scrollPaneBounds,
      padding = scrollPaneVerticalPadding,
      placeAtBottom = true,
      enableVerticalScroll = active,
      transparentBackground = !active
    )
    for ((ts, m), index) <- texts.zipWithIndex do {
      val (t, l) = makeText(m, index)
      pane.addComponent(makeMessageLabel(t, l, ts, active))
    }
    pane.scroll(0, 1e9) // scroll to bottom so the newest messages are visible
    pane
  }

  private def makeMessageLabel(text: Text, location: LocationInfo, ts: Long, active: Boolean) = {
    val label = new Label(text, location)
    if active then label else label.withFade(ts + 5000, 1000)
  }

  def addMessage(m: ServerMessage): Unit = {
    val ts = System.currentTimeMillis()

    val (text, location) = makeText(m, texts.size)

    if scrollPane.isDefined then {
      val active = input.isDefined
      scrollPane.get.addComponent(makeMessageLabel(text, location, ts, active))
      scrollPane.get.scroll(0, 0.06f)
    }
    texts += ((ts, m))
  }

  def makeText(m: ServerMessage, index: Int): (Text, LocationInfo) = {
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

    val location = LocationInfo.from16x9(0.01f, -0.03f * index, 0.3f, 0.03f)
    (
      Component.makeText(
        prefix + m.text,
        location,
        2,
        centered = false,
        shadow = true,
        bold = bold,
        color = color
      ),
      location
    )
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

    if enabled then {
      if scrollPane.isDefined then removeComponent(scrollPane.get)
      scrollPane = Some(makeScrollPane(active = true))
      addComponent(scrollPane.get)
    } else if !enabled then {
      if scrollPane.isDefined then removeComponent(scrollPane.get)
      scrollPane = Some(makeScrollPane(active = false))
      addComponent(scrollPane.get)
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
}
