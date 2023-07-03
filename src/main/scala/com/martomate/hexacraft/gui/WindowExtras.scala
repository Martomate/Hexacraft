package com.martomate.hexacraft.gui

import com.martomate.hexacraft.infra.CursorMode

trait WindowExtras {
  def resetMousePos(): Unit
  def setCursorMode(cursorMode: CursorMode): Unit
}
