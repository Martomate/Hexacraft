package com.martomate.hexacraft.scene

import java.io.File

import com.martomate.hexacraft.GameWindow

trait GameWindowExtended extends GameWindow {
  def scenes: SceneStack

  def saveFolder: File

  def resetMousePos(): Unit

  def setCursorLayout(cursorLayout: Int): Unit

  def tryQuit(): Unit
}
