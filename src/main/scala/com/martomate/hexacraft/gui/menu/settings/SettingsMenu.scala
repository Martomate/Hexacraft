package com.martomate.hexacraft.gui.menu.settings

import com.martomate.hexacraft.Main
import com.martomate.hexacraft.gui.comp.Button
import com.martomate.hexacraft.gui.location.LocationInfo
import com.martomate.hexacraft.gui.menu.MenuScene

class SettingsMenu extends MenuScene {
  addComponent(new Button("Coming soon!", LocationInfo(0.4f, 0.55f, 0.2f, 0.1f))({ println("Settings will be implemented soon") }))
  addComponent(new Button("Back to menu", LocationInfo(0.4f, 0.25f, 0.2f, 0.1f))({ Main.popScene() }))

  override def onReloadedResources(): Unit = ()
}
