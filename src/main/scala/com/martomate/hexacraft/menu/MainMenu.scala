package com.martomate.hexacraft.menu

import com.martomate.hexacraft.{GameKeyboard, GameMouse}
import com.martomate.hexacraft.gui.{GameWindowExtended, LocationInfo, MenuScene}
import com.martomate.hexacraft.gui.comp.{Button, Label}
import com.martomate.hexacraft.world.block.Blocks

import java.io.File

class MainMenu(saveFolder: File, tryQuit: () => Unit)(using
    mouse: GameMouse,
    keyboard: GameKeyboard,
    window: GameWindowExtended,
    Blocks: Blocks
) extends MenuScene {
  addComponent(new Label("Hexacraft", LocationInfo.from16x9(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
  addComponent(Button("Play", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f)) {
    window.pushScene(new WorldChooserMenu(saveFolder))
  })
  /*
  addComponent(Button("Multiplayer", LocationInfo16x9(0.4f, 0.4f, 0.2f, 0.1f)) {
    window.scenes.pushScene(new MultiplayerMenu)
  })
   */
  addComponent(Button("Settings", LocationInfo.from16x9(0.4f, 0.4f, 0.2f, 0.1f)) {
    window.pushScene(new SettingsMenu)
  })
  addComponent(Button("Quit", LocationInfo.from16x9(0.4f, 0.05f, 0.2f, 0.1f)) {
    tryQuit()
  })

  override def isMainMenu: Boolean = true
}
