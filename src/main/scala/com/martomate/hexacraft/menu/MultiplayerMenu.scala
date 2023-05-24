package com.martomate.hexacraft.menu

import com.martomate.hexacraft.{GameMouse, GameWindow}
import com.martomate.hexacraft.gui.{LocationInfo, MenuScene, WindowScenes}
import com.martomate.hexacraft.gui.comp.{Button, Label}

import java.io.File

class MultiplayerMenu(saveFolder: File)(using mouse: GameMouse, window: GameWindow, scenes: WindowScenes)
    extends MenuScene {
  addComponent(new Label("Multiplayer", LocationInfo.from16x9(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
  addComponent(Button("Join", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f)) {
    scenes.pushScene(new JoinWorldChooserMenu)
  })
  addComponent(Button("Host", LocationInfo.from16x9(0.4f, 0.4f, 0.2f, 0.1f)) {
    scenes.pushScene(new HostWorldChooserMenu(saveFolder))
  })
  addComponent(Button("Back", LocationInfo.from16x9(0.4f, 0.05f, 0.2f, 0.1f))(scenes.popScene()))
}
