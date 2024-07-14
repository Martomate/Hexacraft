package hexacraft.main

import hexacraft.client.FakeBlockTextureLoader
import hexacraft.game.GameKeyboard
import hexacraft.gui.{Event, MousePosition, TickContext, WindowSize}
import hexacraft.gui.Event.{KeyEvent, MouseClickEvent}
import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.window.*
import hexacraft.util.Tracker
import hexacraft.world.{CylinderSize, FakeWorldProvider, Inventory, Player}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.{ChunkData, SparseChunkStorage}
import hexacraft.world.coord.BlockRelChunk

import munit.FunSuite
import org.joml.{Vector2f, Vector2i}

import java.util.UUID

class GameSceneTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private val windowSize = WindowSize(Vector2i(1920, 1080), Vector2i(1920, 1080))

  test("GameScene.unload frees all shader programs owned by the GameScene") {
    OpenGL._enterTestMode()

    val worldProvider = new FakeWorldProvider(123L)
    val textureLoader = new FakeBlockTextureLoader

    // Load and unload the game (to ensure static shaders are loaded)
    val keyboard: GameKeyboard = _ => false
    val audioSystem = AudioSystem.createNull()

    val (gameScene1, _) =
      GameScene
        .create(
          GameScene
            .ClientParams(
              UUID.randomUUID(),
              "localhost",
              19271,
              false,
              keyboard,
              textureLoader,
              audioSystem,
              windowSize
            ),
          Some(GameScene.ServerParams(worldProvider))
        )
        .unwrap()
    gameScene1.unload()

    // Start listening to OpenGL events
    val tracker = Tracker.withStorage[OpenGL.Event]
    OpenGL.trackEvents(tracker)

    // Load and unload the game again
    val (gameScene, _) = GameScene
      .create(
        GameScene
          .ClientParams(UUID.randomUUID(), "localhost", 19272, false, keyboard, textureLoader, audioSystem, windowSize),
        Some(GameScene.ServerParams(worldProvider))
      )
      .unwrap()
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
    val keyboard: GameKeyboard = _ => false
    val textureLoader = new FakeBlockTextureLoader
    val audioSystem = AudioSystem.createNull()

    val (gameScene, rx) = GameScene
      .create(
        GameScene
          .ClientParams(UUID.randomUUID(), "localhost", 19273, false, keyboard, textureLoader, audioSystem, windowSize),
        Some(GameScene.ServerParams(worldProvider))
      )
      .unwrap()
    val gameSceneTracker = Tracker.fromRx(rx)

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

    // Step 1: configure the server to host a world with a player looking at a block a few meters away

    val playerId = UUID.randomUUID()
    val storedPlayer = new Player(playerId, Inventory.default)
    storedPlayer.flying = false
    storedPlayer.position.set(0, 4, 0) // ensure player is in the middle of the spawn chunk (4 meters = 8 blocks)
    storedPlayer.rotation.set(math.Pi / 2, 0, 0) // look down

    val spawnChunkBlocks = new SparseChunkStorage
    spawnChunkBlocks.setBlock(BlockRelChunk(0, 0, 0), new BlockState(Block.Dirt, 0))

    val worldProvider = new FakeWorldProvider(123L)
    worldProvider.saveState(storedPlayer.toNBT, "", s"players/${playerId.toString}.dat")
    worldProvider.saveState(ChunkData.fromStorage(spawnChunkBlocks).toNBT, "chunk", "data/" + 0 + "/" + 0 + ".dat")

    // Step 2: configure the client

    val keyboard: GameKeyboard = _ => false
    val textureLoader = new FakeBlockTextureLoader
    val audioSystem = AudioSystem.createNull()

    val (gameScene, rx) = GameScene
      .create(
        GameScene
          .ClientParams(playerId, "localhost", 19274, false, keyboard, textureLoader, audioSystem, windowSize),
        Some(GameScene.ServerParams(worldProvider))
      )
      .unwrap()
    val gameSceneTracker = Tracker.fromRx(rx)

    // Step 3: perform the test scenario

    // ensure the spawn chunk gets loaded
    val tickContext = TickContext(windowSize, MousePosition(Vector2f(0, 0)), MousePosition(Vector2f(0, 0)))
    gameScene.tick(tickContext)
    Thread.sleep(20)
    gameScene.tick(tickContext)

    println(gameScene.client.player.position)

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

    // Step 1: configure the server to host a world with a player looking at a block a few meters away

    val playerId = UUID.randomUUID()
    val storedPlayer = new Player(playerId, Inventory.default)
    storedPlayer.flying = false
    storedPlayer.position.set(0, 4, 0) // ensure player is in the middle of the spawn chunk (4 meters = 8 blocks)
    storedPlayer.rotation.set(math.Pi / 2, 0, 0) // look down

    val spawnChunkBlocks = new SparseChunkStorage
    spawnChunkBlocks.setBlock(BlockRelChunk(0, 0, 0), new BlockState(Block.Dirt, 0))

    val worldProvider = new FakeWorldProvider(123L)
    worldProvider.saveState(storedPlayer.toNBT, "", s"players/${playerId.toString}.dat")
    worldProvider.saveState(ChunkData.fromStorage(spawnChunkBlocks).toNBT, "chunk", "data/" + 0 + "/" + 0 + ".dat")

    // Step 2: configure the client

    val keyboard: GameKeyboard = _ => false
    val textureLoader = new FakeBlockTextureLoader
    val audioSystem = AudioSystem.createNull()

    val (gameScene, rx) = GameScene
      .create(
        GameScene
          .ClientParams(playerId, "localhost", 19275, false, keyboard, textureLoader, audioSystem, windowSize),
        Some(GameScene.ServerParams(worldProvider))
      )
      .unwrap()
    val gameSceneTracker = Tracker.fromRx(rx)

    // Step 3: perform the test scenario

    // ensure the spawn chunk gets loaded
    val tickContext = TickContext(windowSize, MousePosition(Vector2f(0, 0)), MousePosition(Vector2f(0, 0)))
    gameScene.tick(tickContext)
    Thread.sleep(20)
    gameScene.tick(tickContext)

    // start listening for audio events
    val audioTracker = Tracker.withStorage[AudioSystem.Event]
    audioSystem.trackEvents(audioTracker)

    // place block
    gameScene.handleEvent(MouseClickEvent(MouseButton.Right, MouseAction.Press, KeyMods.none, (0, 0)))
    gameScene.tick(tickContext)

    // the sound should have started playing
    assertEquals(audioTracker.events, Seq(AudioSystem.Event.StartedPlaying))
  }
}
