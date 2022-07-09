package com.martomate.hexacraft.main

import com.martomate.hexacraft.GameMouse
import org.joml.{Vector2d, Vector2dc}

class RealGameMouse extends GameMouse {
  private val mousePos = new Vector2d()
  private val mouseMoved = new Vector2d()
  private var skipMouseMovedUpdate = false

  override def pos: Vector2dc = mousePos

  override def moved: Vector2dc = mouseMoved

  def skipNextMouseMovedUpdate(): Unit = skipMouseMovedUpdate = true

  def moveTo(x: Double, y: Double): Unit = {
    if (!skipMouseMovedUpdate) mouseMoved.set(x - pos.x, y - pos.y)

    if (x != pos.x || y != pos.y)
      skipMouseMovedUpdate = false

    mousePos.set(x, y)
  }
}
