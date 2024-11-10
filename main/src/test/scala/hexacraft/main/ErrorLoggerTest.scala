package hexacraft.main

import hexacraft.infra.fs.FileSystem

import munit.FunSuite

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.nio.file.Path

class ErrorLoggerTest extends FunSuite {
  test("does not write log files in debug mode") {
    val fs = FileSystem.createNull()
    val tracker = fs.trackWrites()

    val logger = ErrorLogger.ToConsole
    captureStdErr(logger.log(new Exception("something happened")))

    assertEquals(tracker.events, Seq())
  }

  test("writes stacktrace to stderr in debug mode") {
    val fs = FileSystem.createNull()
    val logger = ErrorLogger.ToConsole

    val output = captureStdErr(logger.log(new Exception("something happened")))

    val lines = output.linesIterator.toList
    assert(lines.size > 1)
    assertEquals(lines(0), "java.lang.Exception: something happened")
  }

  test("write a log file in release mode") {
    val fs = FileSystem.createNull()
    val tracker = fs.trackWrites()

    val logger = ErrorLogger.ToFile(File("some/path/logs"), fs)
    captureStdErr(logger.log(new Exception("something happened")))

    assertEquals(tracker.events.size, 1)
    val writeEvent = tracker.events(0)

    val logsFolder = writeEvent.path.getParent
    assertEquals(logsFolder, Path.of("some/path/logs"))

    val logFileName = writeEvent.path.getFileName.toString
    assert(logFileName.startsWith("error_"))
    assert(logFileName.endsWith(".log"))

    val logLines = String(writeEvent.bytes.toArray).linesIterator.toList
    assert(logLines.size > 1)
    assertEquals(logLines(0), "java.lang.Exception: something happened")
  }

  // TODO: the check below should probably live in FileSystem instead
  // On Windows colons may not be used in file names
  test("does not use colon in the log file name") {
    val fs = FileSystem.createNull()
    val tracker = fs.trackWrites()

    val logger = ErrorLogger.ToFile(File("some/path/logs"), fs)
    captureStdErr(logger.log(new Exception("something happened")))

    assertEquals(tracker.events.size, 1)

    val logFile = tracker.events(0).path
    val logFileName = logFile.getFileName.toString
    assert(!logFileName.contains(':'), s"'$logFileName' contains a colon")
  }

  test("writes to stderr to mention the log file in release mode") {
    val fs = FileSystem.createNull()
    val tracker = fs.trackWrites()

    val logger = ErrorLogger.ToFile(File("some/path/logs"), fs)

    val output = captureStdErr(logger.log(new Exception("something happened")))

    assertEquals(tracker.events.size, 1)
    val writeEvent = tracker.events(0)
    val logFileAbsolutePath = writeEvent.path.toAbsolutePath.toString

    assert(output.contains(logFileAbsolutePath))
  }

  def captureStdErr(code: => Unit): String = {
    val prevErr = System.err
    try
      val err = ByteArrayOutputStream()
      System.setErr(PrintStream(err))

      code

      String(err.toByteArray)
    finally System.setErr(prevErr)
  }
}
