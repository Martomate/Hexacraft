package hexacraft.game

import hexacraft.gui.{Event, MousePosition, TickContext, WindowSize}
import hexacraft.gui.Event.{KeyEvent, MouseClickEvent}
import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.window.*
import hexacraft.util.Tracker
import hexacraft.world.{CylinderSize, FakeWorldProvider}

import munit.FunSuite
import org.joml.{Vector2f, Vector2i}

class GameSceneTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private val windowSize = WindowSize(Vector2i(1920, 1080), Vector2i(1920, 1080))

  test("GameScene.unload frees all shader programs owned by the GameScene") {
    OpenGL._enterTestMode()

    val worldProvider = new FakeWorldProvider(123L)
    val textureLoader = new FakeBlockTextureLoader

    // Load and unload the game (to ensure static shaders are loaded)
    val networkHandler = NetworkHandler(true, false, worldProvider, null)
    val keyboard: GameKeyboard = _ => false
    val audioSystem = AudioSystem.createNull()

    val gameScene1 = GameScene.create(networkHandler, keyboard, textureLoader, windowSize, audioSystem)(_ => ())
    gameScene1.unload()

    // Start listening to OpenGL events
    val tracker = Tracker.withStorage[OpenGL.Event]
    OpenGL.trackEvents(tracker)

    // Load and unload the game again
    val gameScene = GameScene.create(networkHandler, keyboard, textureLoader, windowSize, audioSystem)(_ => ())
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

    val worldProvider = new FakeWorldProvider(123L)
    val networkHandler = NetworkHandler(true, false, worldProvider, null)
    val keyboard: GameKeyboard = _ => false
    val textureLoader = new FakeBlockTextureLoader
    val audioSystem = AudioSystem.createNull()

    val gameSceneTracker = Tracker.withStorage[GameScene.Event]
    val gameScene = GameScene.create(networkHandler, keyboard, textureLoader, windowSize, audioSystem)(gameSceneTracker)

    gameScene.handleEvent(Event.KeyEvent(KeyboardKey.Escape, 0, KeyAction.Press, KeyMods.none))

    gameScene.handleEvent(Event.MouseClickEvent(MouseButton.Left, MouseAction.Press, KeyMods.none, (0, -0.4f)))
    gameScene.handleEvent(Event.MouseClickEvent(MouseButton.Left, MouseAction.Release, KeyMods.none, (0, -0.4f)))

    assertEquals(
      gameSceneTracker.events,
      Seq(
        GameScene.Event.CursorCaptured, // when the game starts
        GameScene.Event.CursorReleased, // when Escape is pressed
        GameScene.Event.GameQuit // when the "Back to Menu" button is pressed
      )
    )
  }

  test("GameScene plays a sound when the player breaks a block") {
    OpenGL._enterTestMode()

    val worldProvider = new FakeWorldProvider(123L)
    val networkHandler = NetworkHandler(true, false, worldProvider, null)
    val keyboard: GameKeyboard = _ => false
    val textureLoader = new FakeBlockTextureLoader
    val audioSystem = AudioSystem.createNull()

    val gameScene = GameScene.create(networkHandler, keyboard, textureLoader, windowSize, audioSystem)(_ => ())

    gameScene.player.flying = false

    // ensure player is in the middle of the spawn chunk
    gameScene.player.position.y = 4 // each block is 0.5 high

    // look down
    gameScene.player.rotation.set(math.Pi / 2, 0, 0)

    // ensure the spawn chunk gets loaded
    val tickContext = TickContext(windowSize, MousePosition(Vector2f(0, 0)), MousePosition(Vector2f(0, 0)))
    gameScene.tick(tickContext)
    Thread.sleep(20)
    gameScene.tick(tickContext)

    // place block below feet
    gameScene.handleEvent(KeyEvent(KeyboardKey.Letter('B'), 0, KeyAction.Press, KeyMods.none))
    gameScene.tick(tickContext)

    // start listening for audio events
    val audioTracker = Tracker.withStorage[AudioSystem.Event]
    audioSystem.trackEvents(audioTracker)

    // break block
    gameScene.handleEvent(MouseClickEvent(MouseButton.Left, MouseAction.Press, KeyMods.none, (0, 0)))
    gameScene.tick(tickContext)

    // the sound should have started playing
    assertEquals(audioTracker.events, Seq(AudioSystem.Event.StartedPlaying))
  }

  test("GameScene plays a sound when the player places a block") {
    OpenGL._enterTestMode()

    val worldProvider = new FakeWorldProvider(123L)
    val networkHandler = NetworkHandler(true, false, worldProvider, null)
    val keyboard: GameKeyboard = _ => false
    val textureLoader = new FakeBlockTextureLoader
    val audioSystem = AudioSystem.createNull()

    val gameScene = GameScene.create(networkHandler, keyboard, textureLoader, windowSize, audioSystem)(_ => ())

    gameScene.player.flying = false

    // ensure player is in the middle of the spawn chunk, and far from the ground
    gameScene.player.position.y = 128 + 4 // each block is 0.5 high

    // look down
    gameScene.player.rotation.set(math.Pi / 2, 0, 0)

    // ensure the spawn chunk gets loaded
    val tickContext = TickContext(windowSize, MousePosition(Vector2f(0, 0)), MousePosition(Vector2f(0, 0)))
    gameScene.tick(tickContext)
    Thread.sleep(20)
    gameScene.tick(tickContext)

    // place block below feet
    gameScene.handleEvent(KeyEvent(KeyboardKey.Letter('B'), 0, KeyAction.Press, KeyMods.none))
    gameScene.tick(tickContext)

    // start listening for audio events
    val audioTracker = Tracker.withStorage[AudioSystem.Event]
    audioSystem.trackEvents(audioTracker)

    // move so the block can be placed
    gameScene.player.position.y += 1

    // place block
    gameScene.handleEvent(MouseClickEvent(MouseButton.Right, MouseAction.Press, KeyMods.none, (0, 0)))
    gameScene.tick(tickContext)

    // the sound should have started playing
    assertEquals(audioTracker.events, Seq(AudioSystem.Event.StartedPlaying))
  }
}
