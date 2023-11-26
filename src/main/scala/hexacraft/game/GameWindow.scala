package hexacraft.game

import hexacraft.gui.WindowSize
import hexacraft.infra.window.CursorMode

trait GameWindow:
  def windowSize: WindowSize
  def setCursorMode(cursorMode: CursorMode): Unit
