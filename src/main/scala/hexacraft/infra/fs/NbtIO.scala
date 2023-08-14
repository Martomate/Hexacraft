package hexacraft.infra.fs

import hexacraft.infra.fs.FileSystem
import hexacraft.nbt.Nbt
import hexacraft.util.AsyncFileIO

import com.flowpowered.nbt.{CompoundMap, CompoundTag, Tag}
import hexacraft.math.GzipAlgorithm

import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

class NbtIO(fs: FileSystem) {
  def saveTag(tag: Tag[_], nbtFile: File): Unit =
    val writeResult = AsyncFileIO.submit(
      nbtFile,
      nbtFile => {
        val bytes = Nbt.convertTag(tag).toBinary(tag.getName)
        val compressedBytes = GzipAlgorithm.compress(bytes)

        fs.writeBytes(nbtFile.toPath, compressedBytes)
      }
    )
    Await.result(writeResult, Duration(5, SECONDS))

  def loadTag(file: File): CompoundTag =
    if fs.exists(file.toPath) then
      val readOperation = AsyncFileIO.submit(
        file,
        file => {
          val compressedBytes = fs.readAllBytes(file.toPath).unwrap()
          val bytes = GzipAlgorithm.decompress(compressedBytes)
          val (name, tag) = Nbt.fromBinary(bytes)
          tag.asInstanceOf[Nbt.MapTag].toCompoundTag(name)
        }
      )
      Await.result(readOperation, Duration(5, SECONDS))
    else new CompoundTag("", new CompoundMap())
}
