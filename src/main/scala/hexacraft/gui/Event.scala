package hexacraft.gui

import hexacraft.infra.window.*

enum Event:
  case KeyEvent(key: KeyboardKey, scancode: Int, action: KeyAction, mods: KeyMods)
  case CharEvent(character: Int)
  case MouseClickEvent(button: MouseButton, action: MouseAction, mods: KeyMods, mousePos: (Float, Float))
  case ScrollEvent(xOffset: Float, yOffset: Float, mousePos: (Float, Float))

object Event:
  extension (e: Event.MouseClickEvent)
    def withMouseTranslation(dx: Float, dy: Float): MouseClickEvent =
      val (mx, my) = e.mousePos
      e.copy(mousePos = (mx + dx, my + dy))
