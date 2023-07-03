package com.martomate.hexacraft.gui

import com.martomate.hexacraft.infra.window.{KeyAction, KeyMods, KeyboardKey, MouseAction, MouseButton}

enum Event:
  case KeyEvent(key: KeyboardKey, scancode: Int, action: KeyAction, mods: KeyMods)
  case CharEvent(character: Int)
  case MouseClickEvent(button: MouseButton, action: MouseAction, mods: KeyMods, mousePos: (Float, Float))
  case ScrollEvent(xOffset: Float, yOffset: Float)

object Event:
  extension (e: Event.MouseClickEvent)
    def withMouseTranslation(dx: Float, dy: Float): MouseClickEvent =
      e.copy(mousePos = (e.mousePos._1 + dx, e.mousePos._2 + dy))
