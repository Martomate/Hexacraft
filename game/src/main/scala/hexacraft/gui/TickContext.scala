package hexacraft.gui

import org.joml.Vector2f

case class TickContext(
    windowSize: WindowSize,
    currentMousePosition: MousePosition,
    previousMousePosition: MousePosition
) {
  def mouseMovement: Vector2f = currentMousePosition.pos.sub(previousMousePosition.pos, new Vector2f)
}
