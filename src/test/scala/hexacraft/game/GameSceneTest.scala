package hexacraft.game

import hexacraft.gui.{Event, WindowExtras}
import hexacraft.infra.fs.BlockTextureLoader
import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.window.*
import hexacraft.main.RealGameMouse
import hexacraft.util.Tracker
import hexacraft.world.{CylinderSize, FakeBlockTextureLoader, FakeWorldProvider}

import munit.FunSuite
import org.joml.{Vector2i, Vector2ic}

class GameSceneTest extends FunSuite {
  given CylinderSize = CylinderSize(8)
  given blockLoader: BlockTextureLoader = new FakeBlockTextureLoader

  given GameMouse = new RealGameMouse
  given GameKeyboard = _ => false

  given GameWindow = new GameWindow:
    override def windowSize: Vector2ic = new Vector2i(1920, 1080)
    override def framebufferSize: Vector2ic = new Vector2i(1920, 1080)

  given WindowExtras = new WindowExtras:
    override def resetMousePos(): Unit = ()
    override def setCursorMode(cursorMode: CursorMode): Unit = ()

  test("GameScene.unload frees all shader programs owned by the GameScene") {
    OpenGL._enterTestMode()

    val worldProvider = new FakeWorldProvider(123L)

    // Load and unload the game (to ensure static shaders are loaded)
    new GameScene(worldProvider)(_ => ()).unload()

    val tracker = Tracker.withStorage[OpenGL.Event]
    OpenGL.trackEvents(tracker)

    // Load and unload the game again
    val gameScene = new GameScene(worldProvider)(_ => ())
    gameScene.unload()

    val shadersAdded = tracker.events.collect:
      case OpenGL.Event.ProgramCreated(programId) => programId

    val shadersRemoved = tracker.events.collect:
      case OpenGL.Event.ProgramDeleted(programId) => programId

    assert(shadersAdded.nonEmpty) // just to be sure

    // All newly loaded shader programs should be released after the game is unloaded
    assertEquals(shadersRemoved.sorted, shadersAdded.sorted)
  }

  test("GameScene emits QuitGame event when quit-button is pressed in pause menu") {
    OpenGL._enterTestMode()

    val gameSceneTracker = Tracker.withStorage[GameScene.Event]

    val worldProvider = new FakeWorldProvider(123L)
    val gameScene = new GameScene(worldProvider)(gameSceneTracker)

    gameScene.handleEvent(Event.KeyEvent(KeyboardKey.Escape, 0, KeyAction.Press, KeyMods.none))

    gameScene.handleEvent(Event.MouseClickEvent(MouseButton.Left, MouseAction.Press, KeyMods.none, (0, -0.4f)))
    gameScene.handleEvent(Event.MouseClickEvent(MouseButton.Left, MouseAction.Release, KeyMods.none, (0, -0.4f)))

    assertEquals(gameSceneTracker.events, Seq(GameScene.Event.QuitGame))
  }
}