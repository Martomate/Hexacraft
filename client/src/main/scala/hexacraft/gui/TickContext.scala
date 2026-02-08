package hexacraft.gui

import hexacraft.game.GameKeyboard

import org.joml.Vector2f

case class TickContext(
    windowSize: WindowSize,
    currentMousePosition: MousePosition,
    previousMousePosition: MousePosition,
    keyboard: GameKeyboard,
    private val readClipboard: () => String
) {
  def mouseMovement: Vector2f = currentMousePosition.pos.sub(previousMousePosition.pos, new Vector2f)
  def clipboard: String = readClipboard()
}
