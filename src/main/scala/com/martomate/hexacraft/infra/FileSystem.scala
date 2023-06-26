package com.martomate.hexacraft.infra

import com.martomate.hexacraft.util.{EventDispatcher, Result, Tracker, TrackerWithStorage}
import com.martomate.hexacraft.util.Result.{Err, Ok}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, InputStream, OutputStream}
import java.nio.file.{Files, NoSuchFileException, Path}
import scala.collection.immutable.ArraySeq

object FileSystem {
  def create(): FileSystem = new FileSystem(RealFiles)

  def createNull(existingFiles: Map[Path, Array[Byte]] = Map.empty): FileSystem =
    new FileSystem(new NullFiles(existingFiles))

  case class FileWrittenEvent(path: Path, bytes: ArraySeq[Byte])

  enum Error:
    case FileNotFound
}

class FileSystem private (files: FilesWrapper) {
  private val dispatcher = new EventDispatcher[FileSystem.FileWrittenEvent]()
  def trackWrites(): TrackerWithStorage[FileSystem.FileWrittenEvent] =
    val tracker = Tracker.withStorage[FileSystem.FileWrittenEvent]
    this.dispatcher.track(tracker)
    tracker

  def writeBytes(path: Path, bytes: Array[Byte]): Unit =
    this.dispatcher.notify(FileSystem.FileWrittenEvent(path, ArraySeq.unsafeWrapArray(bytes)))

    this.files.createDirectories(path.getParent)
    val stream = this.files.newOutputStream(path)
    try
      stream.write(bytes)
      stream.flush()
    finally stream.close()

  def readAllBytes(path: Path): Result[Array[Byte], FileSystem.Error] =
    var stream: InputStream = null
    try
      stream = this.files.newInputStream(path)
      val bytes = stream.readAllBytes()
      Ok(bytes)
    catch
      case _: NoSuchFileException => Err(FileSystem.Error.FileNotFound)
      case e                      => throw e
    finally if stream != null then stream.close()

  def exists(path: Path): Boolean = this.files.exists(path)
}

trait FilesWrapper {
  def exists(path: Path): Boolean
  def createDirectories(path: Path): Path
  def newInputStream(path: Path): InputStream
  def newOutputStream(path: Path): OutputStream
}

object RealFiles extends FilesWrapper {
  def exists(path: Path): Boolean = Files.exists(path)
  override def createDirectories(path: Path): Path = Files.createDirectories(path)
  override def newInputStream(path: Path): InputStream = Files.newInputStream(path)
  override def newOutputStream(path: Path): OutputStream = Files.newOutputStream(path)
}

class NullFiles(files: Map[Path, Array[Byte]]) extends FilesWrapper {
  def exists(path: Path): Boolean = files.contains(path)

  override def createDirectories(path: Path): Path = path

  override def newInputStream(path: Path): InputStream = new ByteArrayInputStream(
    files.getOrElse(path, Array.empty)
  )
  override def newOutputStream(path: Path): OutputStream = new ByteArrayOutputStream()
}
