package hexacraft.main

import hexacraft.infra.audio.AudioSystem
import hexacraft.infra.fs.FileSystem
import hexacraft.infra.os.OSUtils
import hexacraft.infra.window.WindowSystem

import java.io.File

object Main {
  def main(args: Array[String]): Unit = {
    val isDebugStr = System.getProperty("hexacraft.debug")
    val isDebug = isDebugStr != null && isDebugStr == "true"

    val saveFolder: File = new File(OSUtils.appdataPath, ".hexacraft")

    val errorHandler = MainErrorLogger.create(!isDebug, saveFolder)

    val fs = FileSystem.create()
    val audioSystem = AudioSystem.create()
    val windowSystem = WindowSystem.create()

    val window = new MainWindow(isDebug, saveFolder, fs, audioSystem, windowSystem)
    try {
      window.run()
    } catch {
      case t: Throwable =>
        errorHandler.log(t)
        System.exit(1)
    }
  }
}
