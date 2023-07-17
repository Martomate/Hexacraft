package com.martomate.hexacraft.gui

import com.martomate.hexacraft.infra.window.{KeyAction, KeyboardKey, KeyMods, MouseAction, MouseButton}

enum Event:
  case KeyEvent(key: KeyboardKey, scancode: Int, action: KeyAction, mods: KeyMods)
  case CharEvent(character: Int)
  case MouseClickEvent(button: MouseButton, action: MouseAction, mods: KeyMods, mousePos: (Float, Float))
  case ScrollEvent(xOffset: Float, yOffset: Float)

object Event:
  extension (e: Event.MouseClickEvent)
    def withMouseTranslation(dx: Float, dy: Float): MouseClickEvent =
      val (mx, my) = e.mousePos
      e.copy(mousePos = (mx + dx, my + dy))
