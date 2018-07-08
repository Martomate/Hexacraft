package com.martomate.hexacraft.gui.menu.main

import com.martomate.hexacraft.Main
import com.martomate.hexacraft.gui.comp.{Button, Component, Label, LocationInfo}
import com.martomate.hexacraft.gui.menu.MenuScene
import com.martomate.hexacraft.gui.menu.settings.SettingsMenu
import com.martomate.hexacraft.resource.TextureSingle
import com.martomate.hexacraft.scene.GameScene

class MainMenu extends MenuScene {
  addComponent(new Label("Hexacraft", LocationInfo(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
  addComponent(new Button("Play", LocationInfo(0.4f, 0.55f, 0.2f, 0.1f)) ({
    Main.pushScene(new WorldChooserMenu)
  }))
  addComponent(new Button("Settings", LocationInfo(0.4f, 0.4f, 0.2f, 0.1f)) ({
    Main.pushScene(new SettingsMenu)
  }))
  addComponent(new Button("Quit", LocationInfo(0.4f, 0.1f, 0.2f, 0.1f)) ({
    Main.tryQuit()
  }))

}
