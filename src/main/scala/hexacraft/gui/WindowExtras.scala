package hexacraft.gui

import hexacraft.infra.window.CursorMode

trait WindowExtras {
  def resetMousePos(): Unit
  def setCursorMode(cursorMode: CursorMode): Unit
}
