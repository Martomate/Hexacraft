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
  private var numberOfLines: Int = 0

  private var windowAspectRatio = 16f / 9f

  private var input: Option[TextField] = None

  private val scrollPaneVerticalPadding = 0.005f
  private val scrollPaneBounds = LocationInfo.from16x9(0.005f, 0.20f, 0.3f, scrollPaneVerticalPadding + 0.03f * 16)
  private var scrollPane: ScrollPane = {
    val (pane, lines) = makeScrollPane(active = false)
    numberOfLines = lines
    pane
  }
  addComponent(scrollPane)

  def isInputEnabled: Boolean = input.isDefined

  override def render(context: RenderContext): Unit = {
    if windowAspectRatio != context.windowAspectRatio then {
      windowAspectRatio = context.windowAspectRatio

      scrollPane.unload()
      removeComponent(scrollPane)

      val (pane, lines) = makeScrollPane(input.isDefined)
      scrollPane = pane
      numberOfLines = lines
      addComponent(scrollPane)

      if input.isDefined then {
        val newInput = makeInputTextField()
        input.get.unload()
        removeComponent(input.get)
        input = Some(newInput)
        addComponent(input.get)
      }
    }
    super.render(context)
  }

  private def makeScrollPane(active: Boolean): (ScrollPane, Int) = {
    val pane = ScrollPane(
      scrollPaneBounds.inAspectRatio(16f / 9f, windowAspectRatio),
      padding = scrollPaneVerticalPadding,
      placeAtBottom = true,
      enableVerticalScroll = active,
      transparentBackground = !active
    )
    var index = 0
    for (ts, m) <- texts do {
      val (t, l, lines) = makeText(m, index)
      index += lines
      pane.addComponent(makeMessageLabel(t, l, ts, active))
    }
    pane.scroll(0, 1e9) // scroll to bottom so the newest messages are visible
    (pane, index)
  }

  private def makeMessageLabel(text: Text, location: LocationInfo, ts: Long, active: Boolean) = {
    val label = new Label(text, location)
    if active then label else label.withFade(ts + 5000, 1000)
  }

  def addMessage(m: ServerMessage): Unit = {
    val ts = System.currentTimeMillis()

    val (text, location, lines) = makeText(m, numberOfLines)
    numberOfLines += lines

    val active = input.isDefined
    scrollPane.addComponent(makeMessageLabel(text, location, ts, active))
    // scrollPane.scroll(0, location.h)
    texts += ((ts, m))
  }

  def makeText(m: ServerMessage, index: Int): (Text, LocationInfo, Int) = {
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

    val location1 = LocationInfo.from16x9(0.01f, -0.03f * index, 0.3f, 0.03f).inAspectRatio(16f / 9f, windowAspectRatio)
    val text = Component.makeText(
      prefix + m.text,
      location1,
      2,
      centered = false,
      shadow = true,
      bold = bold,
      color = color
    )
    val location2 = LocationInfo
      .from16x9(0.01f, -0.03f * (index + text.numberOfLines - 1), 0.3f, 0.03f * text.numberOfLines)
      .inAspectRatio(16f / 9f, windowAspectRatio)
    (text, location2, text.numberOfLines)
  }

  private def makeInputTextField(): TextField = {
    TextField(
      LocationInfo.from16x9(0.01f, 0.15f, 0.20f, 0.05f).inAspectRatio(16f / 9f, windowAspectRatio),
      "",
      maxFontSize = 2,
      alwaysFocused = true,
      centered = false,
      backgroundEnabled = false
    )
  }

  def setInputEnabled(enabled: Boolean): Unit = {
    if enabled then {
      if input.isEmpty then {
        val c = makeInputTextField()
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
      removeComponent(scrollPane)
      val (pane, lines) = makeScrollPane(active = true)
      numberOfLines = lines
      scrollPane = pane
      addComponent(scrollPane)
    } else if !enabled then {
      removeComponent(scrollPane)
      val (pane, lines) = makeScrollPane(active = false)
      numberOfLines = lines
      scrollPane = pane
      addComponent(scrollPane)
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
