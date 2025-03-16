package hexacraft.infra.window

enum CallbackEvent {
  case KeyPressed(window: Window, key: KeyboardKey, scancode: Int, action: KeyAction, mods: KeyMods)
  case CharTyped(window: Window, character: Int)
  case MouseClicked(window: Window, button: MouseButton, action: MouseAction, mods: KeyMods)
  case MousePosition(window: Window, x: Double, y: Double)
  case MouseScrolled(window: Window, xOffset: Double, yOffset: Double)
  case WindowResized(window: Window, w: Int, h: Int)
  case WindowFocusChanged(window: Window, focused: Boolean)
  case FrameBufferResized(window: Window, w: Int, h: Int)
}
