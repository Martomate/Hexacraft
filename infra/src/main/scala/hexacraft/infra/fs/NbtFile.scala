package hexacraft.infra.fs

import hexacraft.math.GzipAlgorithm
import hexacraft.nbt.Nbt
import hexacraft.util.AsyncFileIO

import java.io.File

class NbtFile(file: File, fs: FileSystem) {
  def exists: Boolean = {
    fs.exists(file.toPath)
  }

  def readMapTag: (String, Nbt.MapTag) = {
    val compressedBytes = AsyncFileIO.perform(file, file => fs.readAllBytes(file.toPath)).unwrap()
    val (name, tag) = Nbt.fromBinary(GzipAlgorithm.decompress(compressedBytes))
    (name, tag.asMap.get)
  }

  def writeMapTag(tag: Nbt.MapTag, name: String): Unit = {
    val compressedBytes = GzipAlgorithm.compress(tag.toBinary(name))
    AsyncFileIO.perform(file, file => fs.writeBytes(file.toPath, compressedBytes))
  }
}
