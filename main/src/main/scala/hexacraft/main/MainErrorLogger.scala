package hexacraft.main

import hexacraft.infra.fs.FileSystem

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.time.OffsetDateTime

class MainErrorLogger(saveToFile: Boolean, saveFolder: File, fs: FileSystem) {
  def log(e: Throwable): Unit = {
    if saveToFile then {
      logToFile(e)
    } else {
      logToConsole(e)
    }
  }

  private def logToConsole(e: Throwable): Unit = {
    e.printStackTrace()
  }

  private def logToFile(e: Throwable): Unit = {
    val byteStream = new ByteArrayOutputStream()
    e.printStackTrace(new PrintStream(byteStream))

    val now = OffsetDateTime.now()
    val logFile = new File(saveFolder, s"logs/error_$now.log".replace(':', '.'))

    fs.writeBytes(logFile.toPath, byteStream.toByteArray)

    System.err.println(
      s"The program has crashed. The crash report can be found in: ${logFile.getAbsolutePath}"
    )
  }
}

object MainErrorLogger {
  def create(saveToFile: Boolean, saveFolder: File): MainErrorLogger = {
    MainErrorLogger(saveToFile, saveFolder, FileSystem.create())
  }
}
