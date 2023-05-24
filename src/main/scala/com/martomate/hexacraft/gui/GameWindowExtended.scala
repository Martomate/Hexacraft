package com.martomate.hexacraft.gui

import com.martomate.hexacraft.{GameKeyboard, GameWindow}

import java.io.File

trait WindowExtras {
  def resetMousePos(): Unit
  def setCursorLayout(cursorLayout: Int): Unit
}

trait WindowScenes {
  def popScene(): Unit
  def pushScene(scene: Scene): Unit
  def popScenesUntil(predicate: Scene => Boolean): Unit
}
