package com.martomate.hexacraft.game

import com.martomate.hexacraft.{GameKeyboard, GameMouse, GameWindow}
import com.martomate.hexacraft.gui.{Scene, WindowExtras, WindowScenes}
import com.martomate.hexacraft.infra.gpu.OpenGL
import com.martomate.hexacraft.infra.window.CursorMode
import com.martomate.hexacraft.main.RealGameMouse
import com.martomate.hexacraft.util.{CylinderSize, Tracker}
import com.martomate.hexacraft.world.{FakeBlockLoader, FakeWorldProvider}
import com.martomate.hexacraft.world.block.{BlockLoader, Blocks}

import munit.FunSuite
import org.joml.{Vector2i, Vector2ic}

class GameSceneTest extends FunSuite {
  given CylinderSize = CylinderSize(8)
  given BlockLoader = new FakeBlockLoader
  given Blocks: Blocks = new Blocks

  given GameMouse = new RealGameMouse
  given GameKeyboard = _ => false

  given GameWindow = new GameWindow:
    override def windowSize: Vector2ic = new Vector2i(1920, 1080)
    override def framebufferSize: Vector2ic = new Vector2i(1920, 1080)

  given WindowScenes = new WindowScenes:
    override def popScene(): Unit = ()
    override def pushScene(scene: Scene): Unit = ()
    override def popScenesUntil(predicate: Scene => Boolean): Unit = ()

  given WindowExtras = new WindowExtras:
    override def resetMousePos(): Unit = ()
    override def setCursorMode(cursorMode: CursorMode): Unit = ()

  test("GameScene.unload frees all shader programs owned by the GameScene") {
    OpenGL._enterTestMode()

    val worldProvider = new FakeWorldProvider(123L)

    // Load and unload the game (to ensure static shaders are loaded)
    new GameScene(worldProvider).unload()

    val tracker = Tracker.withStorage[OpenGL.Event]
    OpenGL.trackEvents(tracker)

    // Load and unload the game again
    val gameScene = new GameScene(worldProvider)
    gameScene.unload()

    val shadersAdded = tracker.events.collect:
      case OpenGL.Event.ProgramCreated(programId) => programId

    val shadersRemoved = tracker.events.collect:
      case OpenGL.Event.ProgramDeleted(programId) => programId

    assert(shadersAdded.nonEmpty) // just to be sure that

    // All newly loaded shader programs should be released after the game is unloaded
    assertEquals(shadersRemoved.sorted, shadersAdded.sorted)
  }
}
