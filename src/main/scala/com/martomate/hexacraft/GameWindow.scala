package com.martomate.hexacraft

import org.joml.{Vector2f, Vector2fc, Vector2i, Vector2ic}

trait GameWindow:
  def windowSize: Vector2ic
  def framebufferSize: Vector2ic

  def mouse: GameMouse
  def keyboard: GameKeyboard

  def aspectRatio: Float = windowSize.x.toFloat / windowSize.y

  def normalizedMousePos: Vector2fc =
    val x = (mouse.pos.x / windowSize.x * 2 - 1).toFloat
    val y = (mouse.pos.y / windowSize.y * 2 - 1).toFloat
    new Vector2f(x, y)
