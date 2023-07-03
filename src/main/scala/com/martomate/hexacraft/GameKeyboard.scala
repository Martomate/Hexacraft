package com.martomate.hexacraft

import com.martomate.hexacraft.infra.Window

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

  class GlfwKeyboard(window: Window) extends GameKeyboard:
    def keyIsPressed(key: GameKeyboard.Key): Boolean =
      import GameKeyboard.Key.*
      key match
        case MoveForward   => window.isKeyPressed(GLFW.GLFW_KEY_W)
        case MoveBackward  => window.isKeyPressed(GLFW.GLFW_KEY_S)
        case MoveRight     => window.isKeyPressed(GLFW.GLFW_KEY_D)
        case MoveLeft      => window.isKeyPressed(GLFW.GLFW_KEY_A)
        case Jump          => window.isKeyPressed(GLFW.GLFW_KEY_SPACE)
        case Sneak         => window.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)
        case LookUp        => window.isKeyPressed(GLFW.GLFW_KEY_UP)
        case LookDown      => window.isKeyPressed(GLFW.GLFW_KEY_DOWN)
        case LookLeft      => window.isKeyPressed(GLFW.GLFW_KEY_LEFT)
        case LookRight     => window.isKeyPressed(GLFW.GLFW_KEY_RIGHT)
        case TurnHeadLeft  => window.isKeyPressed(GLFW.GLFW_KEY_PAGE_UP)
        case TurnHeadRight => window.isKeyPressed(GLFW.GLFW_KEY_PAGE_DOWN)
        case MoveSlowly    => window.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL)
        case MoveFast      => window.isKeyPressed(GLFW.GLFW_KEY_LEFT_ALT)
        case MoveSuperFast => window.isKeyPressed(GLFW.GLFW_KEY_RIGHT_CONTROL)
        case ResetRotation => window.isKeyPressed(GLFW.GLFW_KEY_DELETE) && window.isKeyPressed(GLFW.GLFW_KEY_R)
