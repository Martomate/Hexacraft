package com.martomate.hexacraft.menu

import com.martomate.hexacraft.{GameMouse, GameWindow}
import com.martomate.hexacraft.gui.{LocationInfo, MenuScene, WindowScenes}
import com.martomate.hexacraft.gui.comp.Button

class SettingsMenu(using mouse: GameMouse, window: GameWindow, scenes: WindowScenes) extends MenuScene {
  addComponent(Button("Coming soon!", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f)) {
    println("Settings will be implemented soon")
  })
  addComponent(Button("Back to menu", LocationInfo.from16x9(0.4f, 0.25f, 0.2f, 0.1f)) {
    scenes.popScene()
  })
}
