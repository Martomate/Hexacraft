package hexacraft.infra.window

import org.lwjgl.glfw.GLFW

object KeyAction:
  def fromGlfw(action: Int): KeyAction =
    action match
      case GLFW.GLFW_PRESS   => KeyAction.Press
      case GLFW.GLFW_RELEASE => KeyAction.Release
      case GLFW.GLFW_REPEAT  => KeyAction.Repeat

enum KeyAction:
  case Press
  case Release
  case Repeat
