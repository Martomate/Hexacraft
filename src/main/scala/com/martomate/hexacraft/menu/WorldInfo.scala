package com.martomate.hexacraft.menu

import com.flowpowered.nbt.CompoundTag
import com.flowpowered.nbt.stream.NBTInputStream
import com.martomate.hexacraft.infra.fs.{FileSystem, NbtIO}
import com.martomate.hexacraft.nbt.{Nbt, NBTUtil}

import java.io.{File, FileInputStream}
import java.nio.file.Path

object WorldInfo {
  def fromFile(saveFile: File, fs: FileSystem): WorldInfo = {
    val nbtFile = saveFile.toPath.resolve("world.dat")
    val io = new NbtIO(fs)
    val nbt = io.loadTag(nbtFile.toFile)

    val name = Nbt
      .from(nbt)
      .getCompoundTag("general")
      .flatMap(general => general.getString("name"))
      .getOrElse(saveFile.getName)

    new WorldInfo(saveFile, name)
  }
}

case class WorldInfo(saveFile: File, name: String)
