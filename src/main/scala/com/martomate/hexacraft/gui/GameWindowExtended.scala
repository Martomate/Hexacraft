package com.martomate.hexacraft.gui

import com.martomate.hexacraft.GameWindow

import java.io.File

trait GameWindowExtended extends GameWindow {
  def scenes: SceneStack

  def resetMousePos(): Unit

  def setCursorLayout(cursorLayout: Int): Unit

  def tryQuit(): Unit
}
