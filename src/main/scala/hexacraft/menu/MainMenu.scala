package hexacraft.menu

import hexacraft.GameMouse
import hexacraft.gui.{LocationInfo, MenuScene}
import hexacraft.gui.comp.{Button, Label}
import hexacraft.menu.MainMenu.Event

object MainMenu {
  enum Event:
    case Play
    case Multiplayer
    case Settings
    case Quit
}

class MainMenu(multiplayerEnabled: Boolean)(onEvent: MainMenu.Event => Unit)(using GameMouse) extends MenuScene {
  addComponent(new Label("Hexacraft", LocationInfo.from16x9(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
  addComponent(Button("Play", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f))(onEvent(Event.Play)))

  if multiplayerEnabled then
    addComponent(Button("Multiplayer", LocationInfo.from16x9(0.4f, 0.4f, 0.2f, 0.1f))(onEvent(Event.Multiplayer)))

  addComponent(
    Button("Settings", LocationInfo.from16x9(0.4f, if multiplayerEnabled then 0.25f else 0.4f, 0.2f, 0.1f))(
      onEvent(Event.Settings)
    )
  )
  addComponent(Button("Quit", LocationInfo.from16x9(0.4f, 0.05f, 0.2f, 0.1f))(onEvent(Event.Quit)))

  override def isMainMenu: Boolean = true
}
