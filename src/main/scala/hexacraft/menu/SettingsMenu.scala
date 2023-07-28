package hexacraft.menu

import hexacraft.GameMouse
import hexacraft.gui.{LocationInfo, MenuScene}
import hexacraft.gui.comp.Button

class SettingsMenu(onBack: () => Unit)(using GameMouse) extends MenuScene {
  addComponent(Button("Coming soon!", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f)) {
    println("Settings will be implemented soon")
  })
  addComponent(Button("Back to menu", LocationInfo.from16x9(0.4f, 0.25f, 0.2f, 0.1f)) {
    onBack()
  })
}
