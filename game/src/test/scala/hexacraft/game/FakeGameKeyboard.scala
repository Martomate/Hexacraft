package hexacraft.game

import scala.collection.mutable

class FakeGameKeyboard(override val pressedKeys: Seq[GameKeyboard.Key]) extends GameKeyboard {
  override def keyIsPressed(key: GameKeyboard.Key) = {
    pressedKeys.contains(key)
  }

  override def refreshPressedKeys(): Unit = ()
}
