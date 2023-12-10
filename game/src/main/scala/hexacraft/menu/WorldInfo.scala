package hexacraft.menu

import hexacraft.infra.fs.{FileSystem, NbtIO}

import java.io.File

object WorldInfo {
  def fromFile(saveFile: File, fs: FileSystem): WorldInfo = {
    val nbtFile = saveFile.toPath.resolve("world.dat")
    val io = new NbtIO(fs)
    val (_, nbt) = io.loadTag(nbtFile.toFile)

    val name = nbt
      .getMap("general")
      .flatMap(general => general.getString("name"))
      .getOrElse(saveFile.getName)

    new WorldInfo(saveFile, name)
  }
}

case class WorldInfo(saveFile: File, name: String)
