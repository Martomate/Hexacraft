package com.martomate.hexacraft.event

trait KeyListener {
  def onKeyEvent(event: KeyEvent): Unit
  def onCharEvent(event: CharEvent): Unit
}

case class KeyEvent(key: Int, scancode: Int, action: Int, mods: Int)
case class CharEvent(character: Int)