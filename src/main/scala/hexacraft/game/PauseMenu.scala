package hexacraft.game

import hexacraft.GameMouse
import hexacraft.gui.{Event, LocationInfo, MenuScene}
import hexacraft.gui.comp.{Button, Component, SubComponents}
import hexacraft.infra.window.{KeyAction, KeyboardKey}
import hexacraft.util.Tracker

object PauseMenu {
  enum Event:
    case Unpause
    case QuitGame
}

class PauseMenu(eventHandler: Tracker[PauseMenu.Event])(using GameMouse) extends Component with SubComponents {
  addComponent(Button("Back to game", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f))(unpause()))
  addComponent(Button("Back to menu", LocationInfo.from16x9(0.4f, 0.25f, 0.2f, 0.1f))(quit()))

  private def unpause(): Unit = eventHandler.notify(PauseMenu.Event.Unpause)
  private def quit(): Unit = eventHandler.notify(PauseMenu.Event.QuitGame)

  override def handleEvent(event: Event): Boolean =
    event match
      case Event.KeyEvent(KeyboardKey.Escape, _, KeyAction.Press, _) =>
        unpause()
      case _ => super.handleEvent(event)
    true
}
