package com.martomate.hexacraft.menu

import com.martomate.hexacraft.GameMouse
import com.martomate.hexacraft.gui.{LocationInfo, GameWindowExtended, MenuScene}
import com.martomate.hexacraft.gui.comp.{Button, Label}

import java.io.File

class MultiplayerMenu(saveFolder: File)(using mouse: GameMouse, window: GameWindowExtended) extends MenuScene {
  addComponent(new Label("Multiplayer", LocationInfo.from16x9(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
  addComponent(Button("Join", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f)) {
    window.pushScene(new JoinWorldChooserMenu)
  })
  addComponent(Button("Host", LocationInfo.from16x9(0.4f, 0.4f, 0.2f, 0.1f)) {
    window.pushScene(new HostWorldChooserMenu(saveFolder))
  })
  addComponent(Button("Back", LocationInfo.from16x9(0.4f, 0.05f, 0.2f, 0.1f))(window.popScene()))
}
