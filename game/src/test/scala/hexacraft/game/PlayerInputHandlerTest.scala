package hexacraft.game

import hexacraft.world.{CylinderSize, Player}
import hexacraft.world.coord.CylCoords
import hexacraft.world.entity.Entity

import munit.FunSuite
import org.joml.{Vector2f, Vector3d}

import java.util.UUID

class PlayerInputHandlerTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  test("tick should update the velocity if MoveForward is pressed") {
    val keyboard = FakeGameKeyboard(Seq(GameKeyboard.Key.MoveForward))
    val player = Player.atStartPos(UUID.randomUUID(), "Some dude", CylCoords(1.23, 2.45, 3.56))
    val handler = new PlayerInputHandler()

    assert(player.velocity.length() == 0)
    handler.tick(player, keyboard.pressedKeys, new Vector2f, 1.0, false, Seq.empty)
    assert(player.velocity.length() > 0)
  }

  test("tick should not update the velocity if no key is pressed") {
    val keyboard = FakeGameKeyboard(Seq.empty)
    val player = Player.atStartPos(UUID.randomUUID(), "Some dude", CylCoords(1.23, 2.45, 3.56))
    val handler = new PlayerInputHandler()

    assert(player.velocity.length() == 0)
    handler.tick(player, keyboard.pressedKeys, new Vector2f, 1.0, false, Seq.empty)
    assert(player.velocity.length() == 0)
  }

  test("tick should not rotate the player if mouse has not moved") {
    val keyboard = FakeGameKeyboard(Seq.empty)
    val player = Player.atStartPos(UUID.randomUUID(), "Some dude", CylCoords(1.23, 2.45, 3.56))
    val handler = new PlayerInputHandler()

    player.rotation.set(0.1, 0.2, 0.3)
    handler.tick(player, keyboard.pressedKeys, new Vector2f, 1.0, false, Seq.empty)

    assertEqualsDouble(player.rotation.x, 0.1, 1e-6)
    assertEqualsDouble(player.rotation.y, 0.2, 1e-6)
    assertEqualsDouble(player.rotation.z, 0.3, 1e-6)
  }

  test("test should rotate the player if mouse has moved") {
    val keyboard = FakeGameKeyboard(Seq.empty)
    val player = Player.atStartPos(UUID.randomUUID(), "Some dude", CylCoords(1.23, 2.45, 3.56))
    val handler = new PlayerInputHandler()

    player.rotation.set(0.1, 0.2, 0.3)
    handler.tick(player, keyboard.pressedKeys, new Vector2f(1, 2), 1.0, false, Seq.empty)

    assertEqualsDouble(player.rotation.x, 0.095, 1e-6)
    assertEqualsDouble(player.rotation.y, 0.2025, 1e-6)
    assertEqualsDouble(player.rotation.z, 0.3, 1e-6)
  }

  test("tick should let a mounted player look up to 90 degrees to the side of the mount") {
    val keyboard = FakeGameKeyboard(Seq.empty)
    val player = Player.atStartPos(UUID.randomUUID(), "Some dude", CylCoords(1.23, 2.45, 3.56))
    val boat = Entity.atStartPos(UUID.randomUUID(), CylCoords(1.23, 2.45, 3.56), "boat", Vector3d(0, -1.0, 0)).unwrap()
    val handler = new PlayerInputHandler()

    player.rotation.set(0, 1.0 + 1.5, 0)
    handler.tick(player, keyboard.pressedKeys, new Vector2f, 1.0, false, Seq(boat))

    assertEqualsDouble(player.rotation.y, 2.5, 1e-6)
  }

  test("tick should limit how far a mounted player can look to the side of the mount") {
    val keyboard = FakeGameKeyboard(Seq.empty)
    val player = Player.atStartPos(UUID.randomUUID(), "Some dude", CylCoords(1.23, 2.45, 3.56))
    val boat = Entity.atStartPos(UUID.randomUUID(), CylCoords(1.23, 2.45, 3.56), "boat", Vector3d(0, -1.0, 0)).unwrap()
    val handler = new PlayerInputHandler()

    player.rotation.set(0, 1.0 + 2.0, 0)
    handler.tick(player, keyboard.pressedKeys, new Vector2f, 1.0, false, Seq(boat))
    assertEqualsDouble(player.rotation.y, 1.0 + math.Pi / 2, 1e-6)

    player.rotation.set(0, 1.0 - 2.0 + 2 * math.Pi, 0)
    handler.tick(player, keyboard.pressedKeys, new Vector2f, 1.0, false, Seq(boat))
    assertEqualsDouble(player.rotation.y, 1.0 - math.Pi / 2 + 2 * math.Pi, 1e-6)
  }

  test("wrapAngle should wrap angles into the range [-pi, pi)") {
    assertEqualsDouble(PlayerInputHandler.wrapAngle(0.5), 0.5, 1e-9)
    assertEqualsDouble(PlayerInputHandler.wrapAngle(-0.5), -0.5, 1e-9)
    assertEqualsDouble(PlayerInputHandler.wrapAngle(2 * math.Pi + 0.5), 0.5, 1e-9)
    assertEqualsDouble(PlayerInputHandler.wrapAngle(-2 * math.Pi - 0.5), -0.5, 1e-9)
    assertEqualsDouble(PlayerInputHandler.wrapAngle(math.Pi), -math.Pi, 1e-9)
  }
}
