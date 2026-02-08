package hexacraft.main

import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.gpu.OpenGL
import hexacraft.infra.window.WindowSystem
import hexacraft.util.Tracker

import munit.FunSuite

class MainWindowTest extends FunSuite {
  test("MainWindow initializes the WindowSystem and creates a window") {
    OpenGL._enterTestMode()
    val fileSystem = FileSystem.createNull(Map())
    val audioSystem = AudioSystem.createNull()
    val windowSystem = WindowSystem.createNull()

    val tracker = Tracker.withStorage[WindowSystem.Event]
    windowSystem.trackEvents(tracker)

    val main = MainWindow(false, null, fileSystem, audioSystem, windowSystem)

    assertEquals(
      tracker.events,
      Seq(
        WindowSystem.Event.Initialized,
        WindowSystem.Event.WindowCreated(width = 960, height = 540)
      )
    )
  }

  test("MainWindow initializes the AudioSystem") {
    OpenGL._enterTestMode()
    val fileSystem = FileSystem.createNull(Map())
    val audioSystem = AudioSystem.createNull()
    val windowSystem = WindowSystem.createNull(WindowSystem.NullConfig(shouldClose = true))

    val tracker = Tracker.withStorage[AudioSystem.Event]
    audioSystem.trackEvents(tracker)

    val main = MainWindow(false, null, fileSystem, audioSystem, windowSystem)
    val router = MainRouter(null, true, fileSystem, main, audioSystem)

    // the audio system is initialized in the run function
    main.run(router)

    assertEquals(tracker.events, Seq(AudioSystem.Event.Initialized))
  }
}
