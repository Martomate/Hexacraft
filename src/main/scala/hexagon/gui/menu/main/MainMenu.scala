package hexagon.gui.menu.main

import hexagon.Main
import hexagon.gui.comp.{Button, Component, Label, LocationInfo}
import hexagon.gui.menu.MenuScene
import hexagon.gui.menu.settings.SettingsMenu
import hexagon.resource.TextureSingle
import hexagon.scene.GameScene

class MainMenu extends MenuScene {
  addComponent(new Label("Hexacraft", LocationInfo(0, 0.8f, 1, 0.2f), 5).withColor(1, 1, 1))
  addComponent(new Button("Play", LocationInfo(0.4f, 0.55f, 0.2f, 0.1f)) ({
    Main.pushScene(new WorldChooserMenu)
  }))
  addComponent(new Button("Settings", LocationInfo(0.4f, 0.4f, 0.2f, 0.1f)) ({
    Main.pushScene(new SettingsMenu)
  }))
  addComponent(new Button("Quit", LocationInfo(0.4f, 0.1f, 0.2f, 0.1f)) ({
    Main.tryQuit()
  }))

  override def onReloadedResources(): Unit = ()

}
