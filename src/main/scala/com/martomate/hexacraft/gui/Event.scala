package com.martomate.hexacraft.gui

enum Event:
  case KeyEvent(key: Int, scancode: Int, action: Int, mods: Int)
  case CharEvent(character: Int)
  case MouseClickEvent(button: Int, action: Int, mods: Int, mousePos: (Float, Float))
  case ScrollEvent(xoffset: Float, yoffset: Float)

object Event:
  extension (e: Event.MouseClickEvent)
    def withMouseTranslation(dx: Float, dy: Float): MouseClickEvent =
      e.copy(mousePos = (e.mousePos._1 + dx, e.mousePos._2 + dy))
