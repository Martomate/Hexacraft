package com.martomate.hexacraft

import java.io.File

import org.joml.{Vector2f, Vector2fc, Vector2ic}

trait GameWindow {
  def windowSize: Vector2ic

  def aspectRatio: Float = windowSize.x.toFloat / windowSize.y

  def scenes: SceneStack
  def mouse: GameMouse
  def keyboard: GameKeyboard
  def saveFolder: File

  def resetMousePos(): Unit
  def normalizedMousePos: Vector2fc = new Vector2f(
    (mouse.pos.x / windowSize.x * 2 - 1).toFloat,
    (mouse.pos.y / windowSize.y * 2 - 1).toFloat
  )

  def setCursorLayout(cursorLayout: Int): Unit
  def tryQuit(): Unit
}
