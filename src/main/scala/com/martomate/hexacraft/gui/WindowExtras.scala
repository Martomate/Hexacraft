package com.martomate.hexacraft.gui

import com.martomate.hexacraft.infra.window.CursorMode

trait WindowExtras {
  def resetMousePos(): Unit
  def setCursorMode(cursorMode: CursorMode): Unit
}
