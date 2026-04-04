package hexacraft.tool

import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.window.WindowSystem
import hexacraft.main.{MainRouter, MainWindow, SceneRoute}
import hexacraft.server.RustGameServer

import java.nio.file.Files

object RustServerExperiment {
  def main(args: Array[String]): Unit = {
    val saveDir = Files.createTempDirectory("hexacraft_world_")

    val windowSystem = WindowSystem.create()
    val audioSystem = AudioSystem.createNull()
    val fs = FileSystem.create()

    var running = true

    val server = RustGameServer.start(true, 1298, saveDir.resolve("test_world"))

    new Thread(() => {
      ToolUtils.runAtSteadyFps(1)(running) {
        // Rust performs the tick on its own, so nothing needs to be done here
      }
      server.stop()
    }).start()

    val gameThread = new Thread(() => {
      val window = MainWindow(true, saveDir.toFile, fs, audioSystem, windowSystem)
      window.setNextScene(SceneRoute.Game(null, false, true, ("127.0.0.1", 1298)))
      val router = MainRouter(saveDir.toFile, true, fs, window, audioSystem)
      try {
        window.run(router)
      } finally {
        running = false
      }
    })

    gameThread.start()

    while running do {
      windowSystem.performCallsAsMainThread()
      Thread.sleep(0, 10000)
    }
  }
}
