package com.martomate.hexacraft.game

import com.martomate.hexacraft.{GameMouse, GameWindow}
import com.martomate.hexacraft.gui.{Event, LocationInfo, MenuScene, WindowScenes}
import com.martomate.hexacraft.gui.comp.Button

import org.lwjgl.glfw.GLFW

class PauseMenu(setPaused: Boolean => Unit)(using mouse: GameMouse, window: GameWindow, scenes: WindowScenes)
    extends MenuScene {
  addComponent(Button("Back to game", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f))(unpause()))
  addComponent(Button("Back to menu", LocationInfo.from16x9(0.4f, 0.25f, 0.2f, 0.1f))(quit()))
  hasDefaultBackground = false

  override def isOpaque: Boolean = false

  private def unpause(): Unit =
    scenes.popScene()
    setPaused(false)

  private def quit(): Unit =
    scenes.popScenesUntil(MenuScene.isMainMenu)
    System.gc()

  override def handleEvent(event: Event): Boolean = event match
    case Event.KeyEvent(GLFW.GLFW_KEY_ESCAPE, _, Event.KeyAction.Press, _) =>
      unpause()
      true
    case _ => super.handleEvent(event)

  override def onReloadedResources(): Unit = ()
}
