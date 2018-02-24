package hexacraft.gui.menu.settings

import hexacraft.Main
import hexacraft.gui.comp.{Button, Component, LocationInfo}
import hexacraft.gui.menu.MenuScene
import hexacraft.resource.TextureSingle

class SettingsMenu extends MenuScene {
  addComponent(new Button("Coming soon!", LocationInfo(0.4f, 0.55f, 0.2f, 0.1f))({ println("Settings will be implemented soon") }))
  addComponent(new Button("Back to menu", LocationInfo(0.4f, 0.25f, 0.2f, 0.1f))({ Main.popScene() }))

  override def onReloadedResources(): Unit = ()
}
