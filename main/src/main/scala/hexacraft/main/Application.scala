package hexacraft.main

import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.os.OS
import hexacraft.infra.window.WindowSystem

import java.io.File
import java.nio.file.Files

class Application(
    config: ApplicationConfig,
    fs: FileSystem,
    audioSystem: AudioSystem,
    windowSystem: WindowSystem
) {
  def run(): Boolean = {
    val saveFolder: File = this.createSaveFolder()

    try {
      var gameIsRunning = true

      val gameThread = new Thread(() => {
        val window = new MainWindow(config.isDebug, saveFolder, fs, audioSystem, windowSystem)
        val router = MainRouter(saveFolder, true, fs, window, audioSystem)

        try {
          window.run(router)
        } finally {
          gameIsRunning = false
        }
      })
      gameThread.start()

      while gameIsRunning do {
        windowSystem.performCallsAsMainThread()
        Thread.sleep(0, 10000)
      }

      true
    } catch {
      case t: Throwable =>
        this.handleTopLevelError(saveFolder, t)
        false
    }
  }

  private def createSaveFolder(): File = {
    if config.useTempSaveFolder then {
      Files.createTempDirectory("hexacraft").toFile
    } else {
      val folder = new File(OS.current.appdataPath, ".hexacraft")
      folder.mkdir()
      folder
    }
  }

  private def handleTopLevelError(saveFolder: File, t: Throwable): Unit = {
    val errorHandler = if !config.isDebug then {
      ErrorLogger.ToFile(new File(saveFolder, "logs"), fs)
    } else {
      ErrorLogger.ToConsole
    }

    try {
      errorHandler.log(t)
    } catch {
      case handlerError: Throwable =>
        t.addSuppressed(handlerError)
        t.printStackTrace()
    }
  }
}

object Application {
  def create(config: ApplicationConfig): Application = {
    new Application(
      config,
      FileSystem.create(),
      AudioSystem.create(),
      WindowSystem.create()
    )
  }
}
