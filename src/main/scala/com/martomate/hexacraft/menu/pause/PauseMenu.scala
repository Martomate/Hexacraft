package com.martomate.hexacraft.menu.pause

import com.martomate.hexacraft.gui.{GameWindowExtended, KeyEvent, LocationInfo16x9}
import com.martomate.hexacraft.gui.comp.Button
import com.martomate.hexacraft.menu.MenuScene
import org.lwjgl.glfw.GLFW

class PauseMenu(setPaused: Boolean => Unit)(implicit window: GameWindowExtended) extends MenuScene {
  addComponent(Button("Back to game", LocationInfo16x9(0.4f, 0.55f, 0.2f, 0.1f))(unpause()))
  addComponent(Button("Back to menu", LocationInfo16x9(0.4f, 0.25f, 0.2f, 0.1f))(quit()))
  hasDefaultBackground = false

  override def isOpaque: Boolean = false

  private def unpause(): Unit = {
    window.scenes.popScene()
    setPaused(false)
  }

  private def quit(): Unit = {
    window.scenes.popScenesUntil(MenuScene.isMainMenu)
    System.gc()
  }

  override def onKeyEvent(event: KeyEvent): Boolean = {
    if (event.action == GLFW.GLFW_PRESS && event.key == GLFW.GLFW_KEY_ESCAPE) {
      unpause()
      true
    } else super.onKeyEvent(event)
  }

  override def onReloadedResources(): Unit = ()
}
