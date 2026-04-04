package hexacraft.infra.window

enum CallbackEvent {
  case KeyPressed(key: KeyboardKey, scancode: Int, action: KeyAction, mods: KeyMods)
  case CharTyped(character: Int)
  case MouseClicked(button: MouseButton, action: MouseAction, mods: KeyMods)
  case MousePosition(x: Double, y: Double)
  case MouseScrolled(xOffset: Double, yOffset: Double)
  case WindowResized(w: Int, h: Int)
  case WindowFocusChanged(focused: Boolean)
  case FrameBufferResized(w: Int, h: Int)
}
