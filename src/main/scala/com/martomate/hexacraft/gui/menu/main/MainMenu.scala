package com.martomate.hexacraft.gui.menu.main

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.gui.comp.{Button, Label}
import com.martomate.hexacraft.gui.location.LocationInfo16x9
import com.martomate.hexacraft.gui.menu.MenuScene
import com.martomate.hexacraft.gui.menu.settings.SettingsMenu

class MainMenu(implicit window: GameWindow) extends MenuScene {
  addComponent(new Label("Hexacraft", LocationInfo16x9(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
  addComponent(new Button("Play", LocationInfo16x9(0.4f, 0.55f, 0.2f, 0.1f)) ({
    window.scenes.pushScene(new WorldChooserMenu)
  }))
  addComponent(new Button("Settings", LocationInfo16x9(0.4f, 0.4f, 0.2f, 0.1f)) ({
    window.scenes.pushScene(new SettingsMenu)
  }))
  addComponent(new Button("Quit", LocationInfo16x9(0.4f, 0.1f, 0.2f, 0.1f)) ({
    window.tryQuit()
  }))

  override def isMainMenu: Boolean = true
}
