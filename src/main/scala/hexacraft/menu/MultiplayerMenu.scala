package hexacraft.menu

import hexacraft.gui.{LocationInfo, MenuScene}
import hexacraft.gui.comp.{Button, Label}
import hexacraft.menu.MultiplayerMenu.Event

object MultiplayerMenu {
  enum Event:
    case Join
    case Host
    case GoBack
}

class MultiplayerMenu(onEvent: MultiplayerMenu.Event => Unit) extends MenuScene {
  addComponent(new Label("Multiplayer", LocationInfo.from16x9(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
  addComponent(Button("Join", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f))(onEvent(Event.Join)))
  addComponent(Button("Host", LocationInfo.from16x9(0.4f, 0.4f, 0.2f, 0.1f))(onEvent(Event.Host)))
  addComponent(Button("Back", LocationInfo.from16x9(0.4f, 0.05f, 0.2f, 0.1f))(onEvent(Event.GoBack)))
}
