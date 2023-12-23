package hexacraft.infra.fs

import hexacraft.infra.fs.FileSystem
import hexacraft.util.Result.Err
import hexacraft.util.Tracker
import munit.FunSuite

import java.io.File
import java.nio.file.{Files, Path}
import scala.collection.immutable.ArraySeq

class FileSystemTest extends FunSuite {
  test("writeBytes writes bytes to the file") {
    val dir = Files.createTempDirectory("test_dir")
    val path = dir.resolve("folder").resolve("abc.dat")

    val fs = FileSystem.create()

    fs.writeBytes(path, Array[Byte](111, 222.toByte))

    assertEquals(Files.readAllBytes(path).toSeq, Seq[Byte](111, 222.toByte))
  }

  test("writeBytes overwrites the file if it already exists and is bigger") {
    val dir = Files.createTempDirectory("test_dir")
    val path = dir.resolve("abc.dat")
    Files.write(path, Array[Byte](1, 2, 3))

    val fs = FileSystem.create()

    fs.writeBytes(path, Array[Byte](111, 222.toByte))

    assertEquals(Files.readAllBytes(path).toSeq, Seq[Byte](111, 222.toByte))
  }

  test("writeBytes overwrites the file if it already exists and is smaller") {
    val dir = Files.createTempDirectory("test_dir")
    val path = dir.resolve("abc.dat")
    Files.write(path, Array[Byte](1))

    val fs = FileSystem.create()

    fs.writeBytes(path, Array[Byte](111, 222.toByte))

    assertEquals(Files.readAllBytes(path).toSeq, Seq[Byte](111, 222.toByte))
  }

  test("readAllBytes reads all bytes from the file") {
    val dir = Files.createTempDirectory("test_dir")
    val path = dir.resolve("abc.dat")
    Files.write(path, Array[Byte](111, 222.toByte))

    val fs = FileSystem.create()

    val readBytes = fs.readAllBytes(path).unwrap()

    assertEquals(readBytes.toSeq, Seq[Byte](111, 222.toByte))
  }

  test("readAllBytes returns an error if the file does not exist") {
    val dir = Files.createTempDirectory("test_dir")
    val path = dir.resolve("abc.dat")
    assert(!path.toFile.exists()) // just to be sure

    val fs = FileSystem.create()

    assertEquals(fs.readAllBytes(path), Err(FileSystem.Error.FileNotFound))
  }

  test("null version of writeBytes does not actually write to disk") {
    val dir = Files.createTempDirectory("test_dir")
    val path = dir.resolve("abc.dat")

    val fs = FileSystem.createNull()

    fs.writeBytes(path, Array[Byte](111, 222.toByte))

    assert(!path.toFile.exists())
  }

  test("null version of readAllBytes does not actually read from disk") {
    val dir = Files.createTempDirectory("test_dir")
    val path = dir.resolve("abc.dat")
    Files.write(path, Array[Byte](111, 222.toByte))

    val fs = FileSystem.createNull()

    val readBytes = fs.readAllBytes(path).unwrap()

    assertEquals(readBytes.toSeq, Seq[Byte]())
  }

  test("null version of readAllBytes can be configured") {
    val path = Path.of("some_dir", "abc.dat")

    val fs = FileSystem.createNull(existingFiles = Map(path -> Array[Byte](111, 222.toByte)))

    val readBytes = fs.readAllBytes(path).unwrap()

    assertEquals(readBytes.toSeq, Seq[Byte](111, 222.toByte))
  }

  test("null version of writeBytes emits an event") {
    val path = Path.of("some_dir", "abc.dat")

    val fs = FileSystem.createNull()
    val tracker = fs.trackWrites()

    fs.writeBytes(path, Array[Byte](111, 222.toByte))

    assertEquals(tracker.events, Seq(FileSystem.FileWrittenEvent(path, ArraySeq(111, 222.toByte))))
  }
}
