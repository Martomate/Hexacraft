package com.martomate.hexacraft.game

import com.martomate.hexacraft.GameKeyboard
import com.martomate.hexacraft.main.RealGameMouse
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.FakeBlockLoader
import com.martomate.hexacraft.world.block.{BlockFactory, BlockLoader, Blocks}
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.player.Player

import org.joml.Vector2d
import org.lwjgl.glfw.GLFW
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.collection.mutable

class PlayerInputHandlerTest extends AnyFlatSpec with Matchers {
  given CylinderSize = CylinderSize(8)
  given BlockLoader = new FakeBlockLoader
  given BlockFactory = new BlockFactory
  given Blocks: Blocks = new Blocks

  "tick" should "ask the keyboard for pressed keys" in {
    import GameKeyboard.Key
    val keyboardCalls = mutable.Set.empty[GameKeyboard.Key]
    val keyboard: GameKeyboard = key =>
      keyboardCalls += key
      key == Key.MoveForward
    val player = Player.atStartPos(CylCoords(1.23, 2.45, 3.56))
    val handler = new PlayerInputHandler(keyboard, player)

    handler.tick(new Vector2d(), 1.0)

    keyboardCalls.toSet should contain(Key.MoveForward)
  }

  it should "not rotate the player if mouse has not moved" in {
    val keyboard: GameKeyboard = _ => false
    val player = Player.atStartPos(CylCoords(1.23, 2.45, 3.56))
    val handler = new PlayerInputHandler(keyboard, player)

    player.rotation.set(0.1, 0.2, 0.3)
    handler.tick(new Vector2d(), 1.0)

    player.rotation.x shouldBe 0.1
    player.rotation.y shouldBe 0.2
    player.rotation.z shouldBe 0.3
  }

  it should "rotate the player if mouse has moved" in {
    val keyboard: GameKeyboard = _ => false
    val player = Player.atStartPos(CylCoords(1.23, 2.45, 3.56))
    val handler = new PlayerInputHandler(keyboard, player)

    player.rotation.set(0.1, 0.2, 0.3)
    handler.tick(new Vector2d(1, 2), 1.0)

    player.rotation.x shouldBe 0.095
    player.rotation.y shouldBe 0.2025
    player.rotation.z shouldBe 0.3
  }
}
