package com.martomate.hexacraft.menu.main

import com.martomate.hexacraft.gui.LocationInfo16x9
import com.martomate.hexacraft.gui.comp.{Button, Label}
import com.martomate.hexacraft.menu.MenuScene
import com.martomate.hexacraft.menu.settings.SettingsMenu
import com.martomate.hexacraft.scene.GameWindowExtended

class MainMenu(implicit window: GameWindowExtended) extends MenuScene {
  addComponent(new Label("Hexacraft", LocationInfo16x9(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
  addComponent(Button("Play", LocationInfo16x9(0.4f, 0.55f, 0.2f, 0.1f)) {
    window.scenes.pushScene(new WorldChooserMenu)
  })
  /*
  addComponent(Button("Multiplayer", LocationInfo16x9(0.4f, 0.4f, 0.2f, 0.1f)) {
    window.scenes.pushScene(new MultiplayerMenu)
  })
   */
  addComponent(Button("Settings", LocationInfo16x9(0.4f, 0.4f, 0.2f, 0.1f)) {
    window.scenes.pushScene(new SettingsMenu)
  })
  addComponent(Button("Quit", LocationInfo16x9(0.4f, 0.05f, 0.2f, 0.1f)) {
    window.tryQuit()
  })

  override def isMainMenu: Boolean = true
}
