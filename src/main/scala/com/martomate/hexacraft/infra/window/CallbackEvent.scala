package com.martomate.hexacraft.infra.window

enum CallbackEvent:
  case KeyPressed(window: Window, key: KeyboardKey, scancode: Int, action: KeyAction, mods: KeyMods)
  case CharTyped(window: Window, character: Int)
  case MouseClicked(window: Window, button: MouseButton, action: MouseAction, mods: KeyMods)
  case MouseScrolled(window: Window, xOffset: Double, yOffset: Double)
  case WindowResized(window: Window, w: Int, h: Int)
  case FramebufferResized(window: Window, w: Int, h: Int)
