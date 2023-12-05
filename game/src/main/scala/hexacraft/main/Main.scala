package hexacraft.main

import hexacraft.infra.os.OSUtils

import org.lwjgl.system.Configuration

import java.io.File

object Main:
  def main(args: Array[String]): Unit =
    setNativesFolder()

    val isDebugStr = System.getProperty("hexacraft.debug")
    val isDebug = isDebugStr != null && isDebugStr == "true"

    val saveFolder: File = new File(OSUtils.appdataPath, ".hexacraft")

    val errorHandler = MainErrorLogger.create(!isDebug, saveFolder)

    val window = new MainWindow(isDebug, saveFolder)
    try window.run()
    catch
      case t: Throwable =>
        errorHandler.log(t)
        System.exit(1)

  private def setNativesFolder(): Unit =
    var file = new File("lib/natives")

    if !file.exists
    then file = new File(OSUtils.nativesPath)

    if file.exists
    then Configuration.LIBRARY_PATH.set(file.getAbsolutePath)
