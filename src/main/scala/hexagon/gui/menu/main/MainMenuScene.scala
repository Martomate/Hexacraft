package hexagon.gui.menu.main

import hexagon.Main
import hexagon.gui.comp.{Button, Component, Label, LocationInfo}
import hexagon.gui.menu.MenuScene
import hexagon.resource.TextureSingle
import hexagon.scene.GameScene

class MainMenuScene extends MenuScene {
  addComponent(new Label("Hexagon", LocationInfo(0, 0.8f, 1, 0.2f), 5).withColor(1, 1, 1))
  addComponent(new Button("Play", LocationInfo(0.4f, 0.55f, 0.2f, 0.1f)) ({
    Main.pushScene(new GameScene)
  }))
  addComponent(new Button("Settings", LocationInfo(0.4f, 0.4f, 0.2f, 0.1f)) ({
//    Main.pushScene(new SettingsMenu)
  }))
  addComponent(new Button("Quit", LocationInfo(0.4f, 0.25f, 0.2f, 0.1f)) ({
    Main.tryQuit()
  }))

  private val entireBackground = LocationInfo(0, 0, 1, 1)

  override def render(): Unit = {
    Component.drawImage(entireBackground, TextureSingle.getTexture("textures/gui/menu/background"))
    super.render()
  }

  override def onReloadedResources(): Unit = ()

}
