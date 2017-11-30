package hexagon.gui.menu.settings

import hexagon.Main
import hexagon.gui.comp.{Button, Component, LocationInfo}
import hexagon.gui.menu.MenuScene
import hexagon.resource.TextureSingle

class SettingsMenu extends MenuScene {
  addComponent(new Button("Coming soon!", LocationInfo(0.4f, 0.55f, 0.2f, 0.1f))({ println("Settings will be implemented soon") }))
  addComponent(new Button("Back to menu", LocationInfo(0.4f, 0.25f, 0.2f, 0.1f))({ Main.popScene() }))

  override def onReloadedResources(): Unit = ()
}
