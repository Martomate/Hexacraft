package hexacraft.main

import hexacraft.infra.fs.FileSystem

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.time.OffsetDateTime

trait ErrorLogger {
  def log(e: Throwable): Unit
}

object ErrorLogger {
  object ToConsole extends ErrorLogger {
    override def log(e: Throwable): Unit = {
      e.printStackTrace()
    }
  }

  class ToFile(parentFolder: File, fs: FileSystem) extends ErrorLogger {
    override def log(e: Throwable): Unit = {
      val byteStream = new ByteArrayOutputStream()
      e.printStackTrace(new PrintStream(byteStream))

      val now = OffsetDateTime.now()
      val logFile = new File(parentFolder, s"error_$now.log".replace(':', '.'))

      fs.writeBytes(logFile.toPath, byteStream.toByteArray)

      System.err.println(
        s"The program has crashed. The crash report can be found in: ${logFile.getAbsolutePath}"
      )
    }
  }
}
