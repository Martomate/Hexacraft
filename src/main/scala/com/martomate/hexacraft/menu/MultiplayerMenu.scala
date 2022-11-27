package com.martomate.hexacraft.menu

import com.martomate.hexacraft.gui.{GameWindowExtended, LocationInfo16x9, MenuScene}
import com.martomate.hexacraft.gui.comp.{Button, Label}

import java.io.File

class MultiplayerMenu(saveFolder: File)(implicit window: GameWindowExtended) extends MenuScene {
  addComponent(new Label("Multiplayer", LocationInfo16x9(0, 0.8f, 1, 0.2f), 10).withColor(1, 1, 1))
  addComponent(Button("Join", LocationInfo16x9(0.4f, 0.55f, 0.2f, 0.1f)) {
    window.scenes.pushScene(new JoinWorldChooserMenu)
  })
  addComponent(Button("Host", LocationInfo16x9(0.4f, 0.4f, 0.2f, 0.1f)) {
    window.scenes.pushScene(new HostWorldChooserMenu(saveFolder))
  })
  addComponent(Button("Back", LocationInfo16x9(0.4f, 0.05f, 0.2f, 0.1f))(window.scenes.popScene()))
}
