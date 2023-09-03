package hexacraft.infra.fs

import hexacraft.math.GzipAlgorithm
import hexacraft.nbt.Nbt
import hexacraft.util.AsyncFileIO

import com.flowpowered.nbt.{CompoundMap, CompoundTag, Tag}

import java.io.File

class NbtIO(fs: FileSystem) {
  def saveTag(tag: Tag[_], nbtFile: File): Unit =
    AsyncFileIO.perform(
      nbtFile,
      nbtFile => {
        val bytes = Nbt.convertTag(tag).toBinary(tag.getName)
        val compressedBytes = GzipAlgorithm.compress(bytes)

        fs.writeBytes(nbtFile.toPath, compressedBytes)
      }
    )

  def loadTag(file: File): CompoundTag =
    if fs.exists(file.toPath) then
      AsyncFileIO.perform(
        file,
        file => {
          val compressedBytes = fs.readAllBytes(file.toPath).unwrap()
          val bytes = GzipAlgorithm.decompress(compressedBytes)
          val (name, tag) = Nbt.fromBinary(bytes)
          tag.asInstanceOf[Nbt.MapTag].toCompoundTag(name)
        }
      )
    else new CompoundTag("", new CompoundMap())
}
