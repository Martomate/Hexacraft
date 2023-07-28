package hexacraft.infra.window

import org.lwjgl.glfw.GLFW

object MouseButton:
  def fromGlfw(button: Int): MouseButton =
    button match
      case GLFW.GLFW_MOUSE_BUTTON_LEFT   => MouseButton.Left
      case GLFW.GLFW_MOUSE_BUTTON_RIGHT  => MouseButton.Right
      case GLFW.GLFW_MOUSE_BUTTON_MIDDLE => MouseButton.Middle
      case _                             => MouseButton.Other(button)

enum MouseButton:
  case Left
  case Right
  case Middle
  case Other(button: Int)
