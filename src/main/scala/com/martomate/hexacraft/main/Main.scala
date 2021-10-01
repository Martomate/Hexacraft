package com.martomate.hexacraft.main

import java.io.{File, FileOutputStream, PrintStream}
import com.martomate.hexacraft.util.os.OSUtils
import org.lwjgl.system.Configuration

import java.time.OffsetDateTime

object Main {
  def main(args: Array[String]): Unit = {
    setNatviesFolder()

    val isDebugStr = System.getProperty("hexacraft.debug")
    val isDebug = isDebugStr != null && isDebugStr == "true"

    val window = new MainWindow(isDebug)
    try {
      window.run()
    } catch {
      case t: Throwable =>
        if (isDebug)
          t.printStackTrace()
        else
          logThrowable(t, window.saveFolder)
        System.exit(1);
    }
  }

  private def logThrowable(e: Throwable, saveFolder: File): Unit = {
    val now = OffsetDateTime.now()
    val logFile = new File(saveFolder, s"logs/error_${now}.log")
    logFile.getParentFile.mkdirs()
    e.printStackTrace(new PrintStream(new FileOutputStream(logFile)))
    System.err.println("The program has crashed. The crash report can be found in: " + logFile.getAbsolutePath)
  }

  private def setNatviesFolder(): Unit = {
    var file = new File("lib/natives")
    if (!file.exists) file = new File(OSUtils.nativesPath)
    if (file.exists()) Configuration.LIBRARY_PATH.set(file.getAbsolutePath)
  }
}
