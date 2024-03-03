package hexacraft.game

import hexacraft.infra.window.{KeyboardKey, Window}

import scala.collection.mutable

trait GameKeyboard {
  def pressedKeys: Seq[GameKeyboard.Key] = {
    val pressed = mutable.ArrayBuffer.empty[GameKeyboard.Key]
    for key <- GameKeyboard.Key.values do {
      if keyIsPressed(key) then {
        pressed += key
      }
    }
    pressed.toSeq
  }

  def keyIsPressed(key: GameKeyboard.Key): Boolean
}

object GameKeyboard {
  enum Key {
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
  }

  class GlfwKeyboard(window: Window) extends GameKeyboard {
    def keyIsPressed(key: GameKeyboard.Key): Boolean = {
      import GameKeyboard.Key.*
      key match {
        case MoveForward   => window.isKeyPressed(KeyboardKey.Letter('W'))
        case MoveBackward  => window.isKeyPressed(KeyboardKey.Letter('S'))
        case MoveRight     => window.isKeyPressed(KeyboardKey.Letter('D'))
        case MoveLeft      => window.isKeyPressed(KeyboardKey.Letter('A'))
        case Jump          => window.isKeyPressed(KeyboardKey.Space)
        case Sneak         => window.isKeyPressed(KeyboardKey.LeftShift)
        case LookUp        => window.isKeyPressed(KeyboardKey.Up)
        case LookDown      => window.isKeyPressed(KeyboardKey.Down)
        case LookLeft      => window.isKeyPressed(KeyboardKey.Left)
        case LookRight     => window.isKeyPressed(KeyboardKey.Right)
        case TurnHeadLeft  => window.isKeyPressed(KeyboardKey.PageUp)
        case TurnHeadRight => window.isKeyPressed(KeyboardKey.PageDown)
        case MoveSlowly    => window.isKeyPressed(KeyboardKey.LeftControl)
        case MoveFast      => window.isKeyPressed(KeyboardKey.LeftAlt)
        case MoveSuperFast => window.isKeyPressed(KeyboardKey.RightControl)
        case ResetRotation => window.isKeyPressed(KeyboardKey.Delete) && window.isKeyPressed(KeyboardKey.Letter('R'))
      }
    }
  }
}
