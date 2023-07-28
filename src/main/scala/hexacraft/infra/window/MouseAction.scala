package hexacraft.infra.window

import org.lwjgl.glfw.GLFW

object MouseAction:
  def fromGlfw(action: Int): MouseAction =
    action match
      case GLFW.GLFW_PRESS   => MouseAction.Press
      case GLFW.GLFW_RELEASE => MouseAction.Release

enum MouseAction:
  case Press
  case Release
