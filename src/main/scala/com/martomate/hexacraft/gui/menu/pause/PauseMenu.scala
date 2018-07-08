package com.martomate.hexacraft.gui.menu.pause

import com.martomate.hexacraft.Main
import com.martomate.hexacraft.event.KeyEvent
import com.martomate.hexacraft.gui.comp.{Button, LocationInfo}
import com.martomate.hexacraft.gui.menu.MenuScene
import com.martomate.hexacraft.scene.GameScene
import org.lwjgl.glfw.GLFW


class PauseMenu(game: GameScene) extends MenuScene {
  addComponent(new Button("Back to game", LocationInfo(0.4f, 0.55f, 0.2f, 0.1f))({
    unpause()
  }))
  addComponent(new Button("Back to menu", LocationInfo(0.4f, 0.25f, 0.2f, 0.1f))({
    Main.popScenesUntilMainMenu()
  }))
  hasDefaultBackground = false

  override def isOpaque: Boolean = false

  private def unpause(): Unit = {
    Main.popScene()
    game.setPaused(false)
  }

  override def onKeyEvent(event: KeyEvent): Boolean = {
    if (event.action == GLFW.GLFW_PRESS && event.key == GLFW.GLFW_KEY_ESCAPE) {
      unpause()
      true
    } else super.onKeyEvent(event)
  }

  override def onReloadedResources(): Unit = ()
}
