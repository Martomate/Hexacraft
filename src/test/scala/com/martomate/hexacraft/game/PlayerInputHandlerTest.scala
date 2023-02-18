package com.martomate.hexacraft.game

import com.martomate.hexacraft.GameKeyboard
import com.martomate.hexacraft.main.RealGameMouse
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.FakeBlockLoader
import com.martomate.hexacraft.world.block.{BlockLoader, Blocks}
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.player.Player

import munit.FunSuite
import org.joml.Vector2d
import org.lwjgl.glfw.GLFW
import scala.collection.mutable

class PlayerInputHandlerTest extends FunSuite {
  given CylinderSize = CylinderSize(8)
  given BlockLoader = new FakeBlockLoader
  given Blocks: Blocks = new Blocks

  test("tick should ask the keyboard for pressed keys") {
    import GameKeyboard.Key
    val keyboardCalls = mutable.Set.empty[GameKeyboard.Key]
    val keyboard: GameKeyboard = key =>
      keyboardCalls += key
      key == Key.MoveForward
    val player = Player.atStartPos(CylCoords(1.23, 2.45, 3.56))
    val handler = new PlayerInputHandler(keyboard, player)

    handler.tick(new Vector2d(), 1.0)

    assert(keyboardCalls.toSet.contains(Key.MoveForward))
  }

  test("tick should not rotate the player if mouse has not moved") {
    val keyboard: GameKeyboard = _ => false
    val player = Player.atStartPos(CylCoords(1.23, 2.45, 3.56))
    val handler = new PlayerInputHandler(keyboard, player)

    player.rotation.set(0.1, 0.2, 0.3)
    handler.tick(new Vector2d(), 1.0)

    assertEqualsDouble(player.rotation.x, 0.1, 1e-6)
    assertEqualsDouble(player.rotation.y, 0.2, 1e-6)
    assertEqualsDouble(player.rotation.z, 0.3, 1e-6)
  }

  test("test should rotate the player if mouse has moved") {
    val keyboard: GameKeyboard = _ => false
    val player = Player.atStartPos(CylCoords(1.23, 2.45, 3.56))
    val handler = new PlayerInputHandler(keyboard, player)

    player.rotation.set(0.1, 0.2, 0.3)
    handler.tick(new Vector2d(1, 2), 1.0)

    assertEqualsDouble(player.rotation.x, 0.095, 1e-6)
    assertEqualsDouble(player.rotation.y, 0.2025, 1e-6)
    assertEqualsDouble(player.rotation.z, 0.3, 1e-6)
  }
}
