package hexacraft.tool

import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.window.WindowSystem
import hexacraft.main.{MainRouter, MainWindow, SceneRoute}
import hexacraft.server.{GameServer, WorldProviderFromFile}
import hexacraft.world.{CylinderSize, WorldGenSettings, WorldInfo}

import java.nio.file.Files

object LanRenderExperiment {
  def main(args: Array[String]): Unit = {
    val saveDir = Files.createTempDirectory("hexacraft_world_")

    val windowSystem = WindowSystem.create()
    val audioSystem = AudioSystem.createNull()
    val fs = FileSystem.create()

    var running = true

    val worldInfo = WorldInfo(2, "test world", CylinderSize(8), WorldGenSettings.fromSeed(1234))
    val worldProvider = WorldProviderFromFile(saveDir.resolve("test_world").toFile, fs)
    val server = GameServer.create(true, 1298, worldInfo, worldProvider, 8 * CylinderSize.y60)
    val serverThread = new Thread(() => {
      runAtSteadyFps(60)(running) {
        server.tick()
      }
      server.unload()
    })

    serverThread.start()

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

  def runAtSteadyFps(fps: Int)(running: => Boolean)(tick: => Unit): Unit = {
    while running do {
      val before = System.currentTimeMillis()
      tick
      val after = System.currentTimeMillis()
      val sleepTime = before + 1000 / fps - after
      if sleepTime > 0 then {
        Thread.sleep(sleepTime)
      }
    }
  }
}
