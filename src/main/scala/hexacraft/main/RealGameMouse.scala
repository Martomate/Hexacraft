package hexacraft.main

import hexacraft.game.GameMouse

import org.joml.{Vector2f, Vector2fc}

class RealGameMouse extends GameMouse:
  private val mousePos = new Vector2f()
  private val mouseMoved = new Vector2f()
  private var skipMouseMovedUpdate = false

  override def pos: Vector2fc = mousePos

  override def moved: Vector2fc = mouseMoved

  def skipNextMouseMovedUpdate(): Unit = skipMouseMovedUpdate = true

  def moveTo(x: Double, y: Double): Unit =
    if !skipMouseMovedUpdate
    then mouseMoved.set(x - pos.x, y - pos.y)

    if x != pos.x || y != pos.y
    then skipMouseMovedUpdate = false

    mousePos.set(x, y)
