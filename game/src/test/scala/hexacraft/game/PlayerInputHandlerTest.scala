package hexacraft.game

import hexacraft.world.{CylinderSize, Player}
import hexacraft.world.coord.CylCoords

import munit.FunSuite
import org.joml.Vector2f

import scala.collection.mutable

class PlayerInputHandlerTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  test("tick should ask the keyboard for pressed keys") {
    import GameKeyboard.Key
    val keyboardCalls = mutable.Set.empty[GameKeyboard.Key]
    val keyboard: GameKeyboard = key =>
      keyboardCalls += key
      key == Key.MoveForward
    val player = Player.atStartPos(CylCoords(1.23, 2.45, 3.56))
    val handler = new PlayerInputHandler()

    handler.tick(player, keyboard.pressedKeys, new Vector2f, 1.0, false)

    assert(keyboardCalls.toSet.contains(Key.MoveForward))
  }

  test("tick should not rotate the player if mouse has not moved") {
    val keyboard: GameKeyboard = _ => false
    val player = Player.atStartPos(CylCoords(1.23, 2.45, 3.56))
    val handler = new PlayerInputHandler()

    player.rotation.set(0.1, 0.2, 0.3)
    handler.tick(player, keyboard.pressedKeys, new Vector2f, 1.0, false)

    assertEqualsDouble(player.rotation.x, 0.1, 1e-6)
    assertEqualsDouble(player.rotation.y, 0.2, 1e-6)
    assertEqualsDouble(player.rotation.z, 0.3, 1e-6)
  }

  test("test should rotate the player if mouse has moved") {
    val keyboard: GameKeyboard = _ => false
    val player = Player.atStartPos(CylCoords(1.23, 2.45, 3.56))
    val handler = new PlayerInputHandler()

    player.rotation.set(0.1, 0.2, 0.3)
    handler.tick(player, keyboard.pressedKeys, new Vector2f(1, 2), 1.0, false)

    assertEqualsDouble(player.rotation.x, 0.095, 1e-6)
    assertEqualsDouble(player.rotation.y, 0.2025, 1e-6)
    assertEqualsDouble(player.rotation.z, 0.3, 1e-6)
  }
}
