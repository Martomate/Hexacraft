package hexacraft.infra.window

import org.lwjgl.glfw.GLFW

opaque type KeyboardKey <: AnyVal = Int

object KeyboardKey {
  private[infra] def fromGlfw(key: Int): KeyboardKey = key
  extension (key: KeyboardKey)
    private[infra] def toGlfw: Int = key
    def isDigit: Boolean = key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9

  object Letter {
    def apply(char: Char): KeyboardKey =
      require(char >= 'A' && char <= 'Z', s"Invalid Letter: $char")
      GLFW.GLFW_KEY_A + (char.toInt - 'A'.toInt)

    def unapply(key: KeyboardKey): Option[Char] =
      if key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z
      then Some(((key - GLFW.GLFW_KEY_A) + 'A'.toInt).toChar)
      else None
  }

  object Digit {
    def apply(digit: Byte): KeyboardKey =
      require(digit >= 0 && digit <= 9, s"Invalid Digit: $digit")
      GLFW.GLFW_KEY_0 + digit

    def unapply(key: KeyboardKey): Option[Byte] =
      if key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9
      then Some((key - GLFW.GLFW_KEY_0).toByte)
      else None
  }

  object Function {
    def apply(number: Byte): KeyboardKey =
      require(number >= 1 && number <= 25, s"Invalid Function key: F$number")
      GLFW.GLFW_KEY_F1 + (number - 1)

    def unapply(key: KeyboardKey): Option[Byte] =
      if key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25
      then Some(((key - GLFW.GLFW_KEY_F1) + 1).toByte)
      else None
  }

  val Space: KeyboardKey = GLFW.GLFW_KEY_SPACE
  val LeftShift: KeyboardKey = GLFW.GLFW_KEY_LEFT_SHIFT
  val RightShift: KeyboardKey = GLFW.GLFW_KEY_RIGHT_SHIFT
  val LeftAlt: KeyboardKey = GLFW.GLFW_KEY_LEFT_ALT
  val RightAlt: KeyboardKey = GLFW.GLFW_KEY_RIGHT_ALT
  val LeftControl: KeyboardKey = GLFW.GLFW_KEY_LEFT_CONTROL
  val RightControl: KeyboardKey = GLFW.GLFW_KEY_RIGHT_CONTROL
  val PageUp: KeyboardKey = GLFW.GLFW_KEY_PAGE_UP
  val PageDown: KeyboardKey = GLFW.GLFW_KEY_PAGE_DOWN
  val Delete: KeyboardKey = GLFW.GLFW_KEY_DELETE
  val Up: KeyboardKey = GLFW.GLFW_KEY_UP
  val Down: KeyboardKey = GLFW.GLFW_KEY_DOWN
  val Left: KeyboardKey = GLFW.GLFW_KEY_LEFT
  val Right: KeyboardKey = GLFW.GLFW_KEY_RIGHT
  val Escape: KeyboardKey = GLFW.GLFW_KEY_ESCAPE
  val Backspace: KeyboardKey = GLFW.GLFW_KEY_BACKSPACE
}
