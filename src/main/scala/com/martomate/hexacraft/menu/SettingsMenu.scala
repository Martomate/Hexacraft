package com.martomate.hexacraft.menu

import com.martomate.hexacraft.gui.{GameWindowExtended, LocationInfo16x9, MenuScene}
import com.martomate.hexacraft.gui.comp.Button

class SettingsMenu(implicit window: GameWindowExtended) extends MenuScene {
  addComponent(Button("Coming soon!", LocationInfo16x9(0.4f, 0.55f, 0.2f, 0.1f)) {
    println("Settings will be implemented soon")
  })
  addComponent(Button("Back to menu", LocationInfo16x9(0.4f, 0.25f, 0.2f, 0.1f)) {
    window.scenes.popScene()
  })

  override def onReloadedResources(): Unit = ()
}
