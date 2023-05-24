package com.martomate.hexacraft.game

import com.martomate.hexacraft.GameMouse
import com.martomate.hexacraft.gui.{LocationInfo, Event, GameWindowExtended, MenuScene}
import com.martomate.hexacraft.gui.comp.Button

import org.lwjgl.glfw.GLFW

class PauseMenu(setPaused: Boolean => Unit)(using mouse: GameMouse, window: GameWindowExtended) extends MenuScene {
  addComponent(Button("Back to game", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f))(unpause()))
  addComponent(Button("Back to menu", LocationInfo.from16x9(0.4f, 0.25f, 0.2f, 0.1f))(quit()))
  hasDefaultBackground = false

  override def isOpaque: Boolean = false

  private def unpause(): Unit = {
    window.popScene()
    setPaused(false)
  }

  private def quit(): Unit = {
    window.popScenesUntil(MenuScene.isMainMenu)
    System.gc()
  }

  override def onKeyEvent(event: Event.KeyEvent): Boolean = {
    if (event.action == Event.KeyAction.Press && event.key == GLFW.GLFW_KEY_ESCAPE) {
      unpause()
      true
    } else super.onKeyEvent(event)
  }

  override def onReloadedResources(): Unit = ()
}
