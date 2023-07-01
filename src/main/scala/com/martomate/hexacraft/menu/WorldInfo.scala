package com.martomate.hexacraft.menu

import com.martomate.hexacraft.util.{Nbt, NBTUtil}

import com.flowpowered.nbt.CompoundTag
import com.flowpowered.nbt.stream.NBTInputStream
import java.io.{File, FileInputStream}

object WorldInfo {
  def fromFile(saveFile: File): WorldInfo = {
    val nbtFile = new File(saveFile, "world.dat")
    val stream = new NBTInputStream(new FileInputStream(nbtFile))
    val nbt = stream.readTag().asInstanceOf[CompoundTag]
    stream.close()

    val name = Nbt
      .from(nbt)
      .getCompoundTag("general")
      .flatMap(general => general.getString("name"))
      .getOrElse(saveFile.getName)
    new WorldInfo(saveFile, name)
  }
}

case class WorldInfo(saveFile: File, name: String)
