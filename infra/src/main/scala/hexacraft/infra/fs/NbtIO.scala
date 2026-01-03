package hexacraft.infra.fs

import hexacraft.math.GzipAlgorithm
import hexacraft.nbt.Nbt
import hexacraft.util.AsyncFileIO

import java.io.File

class NbtIO(fs: FileSystem) {
  def saveTag(tag: Nbt, name: String, nbtFile: File): Unit = {
    AsyncFileIO.perform(nbtFile, nbtFile => doSaveTag(tag, name, nbtFile))
  }

  def loadTag(file: File): Option[(String, Nbt.MapTag)] = {
    if fs.exists(file.toPath) then {
      Some(AsyncFileIO.perform(file, file => doLoadTag(file)))
    } else {
      None
    }
  }

  private def doSaveTag(tag: Nbt, name: String, nbtFile: File): Unit = {
    val bytes = tag.toBinary(name)
    val compressedBytes = GzipAlgorithm.compress(bytes)
    fs.writeBytes(nbtFile.toPath, compressedBytes)
  }

  private def doLoadTag(file: File) = {
    val compressedBytes = fs.readAllBytes(file.toPath).unwrap()
    val bytes = GzipAlgorithm.decompress(compressedBytes)
    val (name, tag) = Nbt.fromBinary(bytes)
    (name, tag.asInstanceOf[Nbt.MapTag])
  }
}
