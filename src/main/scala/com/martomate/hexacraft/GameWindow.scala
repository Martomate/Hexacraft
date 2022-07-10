package com.martomate.hexacraft

import org.joml.{Vector2f, Vector2fc, Vector2i, Vector2ic}

trait GameWindow {
  def windowSize: Vector2ic
  def framebufferSize: Vector2ic
  def pixelScale: Vector2ic = {
    val w = windowSize
    val f = framebufferSize
    new Vector2i(f.x / w.x, f.y / w.y)
  }

  def mouse: GameMouse
  def keyboard: GameKeyboard

  def aspectRatio: Float = windowSize.x.toFloat / windowSize.y

  def normalizedMousePos: Vector2fc = new Vector2f(
    (mouse.pos.x / windowSize.x * 2 - 1).toFloat,
    (mouse.pos.y / windowSize.y * 2 - 1).toFloat
  )
}

