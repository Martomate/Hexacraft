package com.martomate.hexacraft

import org.lwjgl.glfw.GLFW

trait GameKeyboard:
  def keyIsPressed(key: GameKeyboard.Key): Boolean

object GameKeyboard:
  enum Key:
    case MoveForward
    case MoveBackward
    case MoveLeft
    case MoveRight

    case Jump
    case Sneak

    case LookUp
    case LookDown
    case LookLeft
    case LookRight
    case TurnHeadLeft
    case TurnHeadRight

    case MoveSlowly
    case MoveFast
    case MoveSuperFast

    case ResetRotation

  class GlfwKeyboard(glfwKeyIsPressed: Int => Boolean) extends GameKeyboard:
    def keyIsPressed(key: GameKeyboard.Key): Boolean =
      import GameKeyboard.Key.*
      key match
        case MoveForward   => glfwKeyIsPressed(GLFW.GLFW_KEY_W)
        case MoveBackward  => glfwKeyIsPressed(GLFW.GLFW_KEY_S)
        case MoveRight     => glfwKeyIsPressed(GLFW.GLFW_KEY_D)
        case MoveLeft      => glfwKeyIsPressed(GLFW.GLFW_KEY_A)
        case Jump          => glfwKeyIsPressed(GLFW.GLFW_KEY_SPACE)
        case Sneak         => glfwKeyIsPressed(GLFW.GLFW_KEY_LEFT_SHIFT)
        case LookUp        => glfwKeyIsPressed(GLFW.GLFW_KEY_UP)
        case LookDown      => glfwKeyIsPressed(GLFW.GLFW_KEY_DOWN)
        case LookLeft      => glfwKeyIsPressed(GLFW.GLFW_KEY_LEFT)
        case LookRight     => glfwKeyIsPressed(GLFW.GLFW_KEY_RIGHT)
        case TurnHeadLeft  => glfwKeyIsPressed(GLFW.GLFW_KEY_PAGE_UP)
        case TurnHeadRight => glfwKeyIsPressed(GLFW.GLFW_KEY_PAGE_DOWN)
        case MoveSlowly    => glfwKeyIsPressed(GLFW.GLFW_KEY_LEFT_CONTROL)
        case MoveFast      => glfwKeyIsPressed(GLFW.GLFW_KEY_LEFT_ALT)
        case MoveSuperFast => glfwKeyIsPressed(GLFW.GLFW_KEY_RIGHT_CONTROL)
        case ResetRotation => glfwKeyIsPressed(GLFW.GLFW_KEY_DELETE) && glfwKeyIsPressed(GLFW.GLFW_KEY_R)
