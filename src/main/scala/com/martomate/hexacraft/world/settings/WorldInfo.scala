package com.martomate.hexacraft.world.settings

import com.martomate.hexacraft.util.{CylinderSize, Nbt, NBTUtil}
import com.martomate.hexacraft.world.MigrationManager

import com.flowpowered.nbt.{ByteTag, CompoundTag, ShortTag, StringTag}
import java.io.File
import scala.collection.immutable.ListMap

class WorldInfo(
    val worldName: String,
    val worldSize: CylinderSize,
    val gen: WorldGenSettings,
    val player: CompoundTag
) {
  def toNBT: CompoundTag = {
    NBTUtil.makeCompoundTag(
      "world",
      Seq(
        new ShortTag("version", MigrationManager.LatestVersion),
        NBTUtil.makeCompoundTag(
          "general",
          Seq(
            new ByteTag("worldSize", worldSize.worldSize.toByte),
            new StringTag("name", worldName)
          )
        ),
        gen.toNBT,
        player
      )
    )
  }
}

object WorldInfo {
  def fromNBT(nbt: CompoundTag, saveDir: File, worldSettings: WorldSettings): WorldInfo = {
    val nbtData = Nbt.from(nbt)
    val generalSettings: Nbt.MapTag =
      NBTUtil.getCompoundTag(nbtData, "general").getOrElse(Nbt.emptyMap)
    val name: String =
      NBTUtil.getString(generalSettings, "name", worldSettings.name.getOrElse(saveDir.getName))
    val size: CylinderSize = CylinderSize(
      NBTUtil.getByte(generalSettings, "worldSize", worldSettings.size.getOrElse(7))
    )
    val gen: WorldGenSettings =
      WorldGenSettings.fromNBT(
        NBTUtil
          .getCompoundTag(nbtData, "gen")
          .map(_.toCompoundTag("gen"))
          .getOrElse(NBTUtil.makeCompoundTag("gen", Seq())),
        worldSettings
      )
    val player: CompoundTag = NBTUtil.getCompoundTag(nbtData, "player").map(_.toCompoundTag("player")).orNull

    new WorldInfo(name, size, gen, player)
  }
}
