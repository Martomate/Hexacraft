package com.martomate.hexacraft.game

import com.martomate.hexacraft.GameMouse
import com.martomate.hexacraft.gui.{Event, LocationInfo, MenuScene}
import com.martomate.hexacraft.gui.comp.Button
import com.martomate.hexacraft.infra.window.{KeyAction, KeyboardKey}

object PauseMenu {
  enum Event:
    case Unpause
    case QuitGame
}

class PauseMenu(onEvent: PauseMenu.Event => Unit)(using GameMouse) extends MenuScene {
  addComponent(Button("Back to game", LocationInfo.from16x9(0.4f, 0.55f, 0.2f, 0.1f))(unpause()))
  addComponent(Button("Back to menu", LocationInfo.from16x9(0.4f, 0.25f, 0.2f, 0.1f))(quit()))
  hasDefaultBackground = false

  override def isOpaque: Boolean = false

  private def unpause(): Unit = onEvent(PauseMenu.Event.Unpause)
  private def quit(): Unit = onEvent(PauseMenu.Event.QuitGame)

  override def handleEvent(event: Event): Boolean = event match
    case Event.KeyEvent(KeyboardKey.Escape, _, KeyAction.Press, _) =>
      unpause()
      true
    case _ => super.handleEvent(event)
}
