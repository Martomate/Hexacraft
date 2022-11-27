package com.martomate.hexacraft.menu

import com.martomate.hexacraft.gui.{GameWindowExtended, LocationInfo16x9, MenuScene}
import com.martomate.hexacraft.gui.comp.{Button, Label}
import com.martomate.hexacraft.world.block.Blocks

import java.io.File

class MainMenu(saveFolder: File)(using window: GameWindowExtended, Blocks: Blocks) extends MenuScene {
  addComponent(new Label("Hexacraft", LocationInfo16x9(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
  addComponent(Button("Play", LocationInfo16x9(0.4f, 0.55f, 0.2f, 0.1f)) {
    window.scenes.pushScene(new WorldChooserMenu(saveFolder))
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
