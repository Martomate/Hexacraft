package com.martomate.hexacraft.gui

import com.martomate.hexacraft.{GameKeyboard, GameWindow}

import java.io.File

trait GameWindowExtended extends GameWindow {
  def resetMousePos(): Unit
  def setCursorLayout(cursorLayout: Int): Unit

  def popScene(): Unit
  def pushScene(scene: Scene): Unit
  def popScenesUntil(predicate: Scene => Boolean): Unit
}
