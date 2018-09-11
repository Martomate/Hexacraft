package com.martomate.hexacraft.gui.menu.settings

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.gui.comp.Button
import com.martomate.hexacraft.gui.location.LocationInfo16x9
import com.martomate.hexacraft.gui.menu.MenuScene

class SettingsMenu(implicit window: GameWindow) extends MenuScene {
  addComponent(new Button("Coming soon!", LocationInfo16x9(0.4f, 0.55f, 0.2f, 0.1f))({ println("Settings will be implemented soon") }))
  addComponent(new Button("Back to menu", LocationInfo16x9(0.4f, 0.25f, 0.2f, 0.1f))({ window.scenes.popScene() }))

  override def onReloadedResources(): Unit = ()
}
