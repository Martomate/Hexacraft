package hexacraft.infra.window

import hexacraft.infra.window.CursorMode.{Disabled, Hidden, Normal}

import org.lwjgl.glfw.GLFW

object CursorMode {
  extension (mode: CursorMode) {
    def toGlfw: Int = mode match {
      case Normal   => GLFW.GLFW_CURSOR_NORMAL
      case Hidden   => GLFW.GLFW_CURSOR_HIDDEN
      case Disabled => GLFW.GLFW_CURSOR_DISABLED
    }
  }
}

enum CursorMode {
  case Normal
  case Hidden
  case Disabled
}
