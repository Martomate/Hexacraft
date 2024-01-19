package hexacraft.main

import hexacraft.gui.MousePosition

import org.joml.Vector2f

class GameMouse {
  private val mousePos = new Vector2f()
  private val mouseMoved = new Vector2f()
  private var skipMouseMovedUpdate = false

  def currentPos: MousePosition = MousePosition(mousePos)

  def previousPos: MousePosition = MousePosition(mousePos.sub(mouseMoved, new Vector2f))

  def skipNextMouseMovedUpdate(): Unit = {
    skipMouseMovedUpdate = true
  }

  def moveTo(x: Double, y: Double): Unit = {
    if !skipMouseMovedUpdate then {
      mouseMoved.set(x - mousePos.x, y - mousePos.y)
    }

    if x != mousePos.x || y != mousePos.y then {
      skipMouseMovedUpdate = false
    }

    mousePos.set(x, y)
  }
}
