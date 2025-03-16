package hexacraft.game

import hexacraft.infra.window.{KeyboardKey, Window}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait GameKeyboard {
  def pressedKeys: Seq[GameKeyboard.Key]

  def keyIsPressed(key: GameKeyboard.Key): Boolean

  def refreshPressedKeys(): Unit
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
    private var currentlyPressedKeys: Set[GameKeyboard.Key] = Set.empty
    private var loadingPressedKeys: Boolean = false

    def pressedKeys: Seq[GameKeyboard.Key] = {
      currentlyPressedKeys.toSeq
    }

    def keyIsPressed(key: GameKeyboard.Key): Boolean = {
      currentlyPressedKeys.contains(key)
    }

    def refreshPressedKeys(): Unit = {
      if !loadingPressedKeys then {
        loadingPressedKeys = true
        Future {
          val pressedPerKey = Seq(
            (Key.MoveForward, window.isKeyPressed(KeyboardKey.Letter('W'))),
            (Key.MoveBackward, window.isKeyPressed(KeyboardKey.Letter('S'))),
            (Key.MoveRight, window.isKeyPressed(KeyboardKey.Letter('D'))),
            (Key.MoveLeft, window.isKeyPressed(KeyboardKey.Letter('A'))),
            (Key.Jump, window.isKeyPressed(KeyboardKey.Space)),
            (Key.Sneak, window.isKeyPressed(KeyboardKey.LeftShift)),
            (Key.LookUp, window.isKeyPressed(KeyboardKey.Up)),
            (Key.LookDown, window.isKeyPressed(KeyboardKey.Down)),
            (Key.LookLeft, window.isKeyPressed(KeyboardKey.Left)),
            (Key.LookRight, window.isKeyPressed(KeyboardKey.Right)),
            (Key.TurnHeadLeft, window.isKeyPressed(KeyboardKey.PageUp)),
            (Key.TurnHeadRight, window.isKeyPressed(KeyboardKey.PageDown)),
            (Key.MoveSlowly, window.isKeyPressed(KeyboardKey.LeftControl)),
            (Key.MoveFast, window.isKeyPressed(KeyboardKey.LeftAlt)),
            (Key.MoveSuperFast, window.isKeyPressed(KeyboardKey.RightControl)),
            (Key.ResetRotation, window.isKeyPressed(KeyboardKey.Delete) && window.isKeyPressed(KeyboardKey.Letter('R')))
          )
          currentlyPressedKeys = pressedPerKey.filter(_._2).map(_._1).toSet
          loadingPressedKeys = false
        }
      }
    }
  }
}
