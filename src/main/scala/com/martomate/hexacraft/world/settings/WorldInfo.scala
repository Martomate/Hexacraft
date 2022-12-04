package com.martomate.hexacraft.world.settings

import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.MigrationManager

import com.flowpowered.nbt.{ByteTag, CompoundTag, ShortTag, StringTag}
import java.io.File

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
  def fromNBT(nbtData: CompoundTag, saveDir: File, worldSettings: WorldSettings): WorldInfo = {
    val generalSettings: CompoundTag = NBTUtil.getCompoundTag(nbtData, "general").orNull
    val name: String =
      NBTUtil.getString(generalSettings, "name", worldSettings.name.getOrElse(saveDir.getName))
    val size: CylinderSize = CylinderSize(
      NBTUtil.getByte(generalSettings, "worldSize", worldSettings.size.getOrElse(7))
    )
    val gen: WorldGenSettings =
      WorldGenSettings.fromNBT(NBTUtil.getCompoundTag(nbtData, "gen").orNull, worldSettings)
    val player: CompoundTag = NBTUtil.getCompoundTag(nbtData, "player").orNull

    new WorldInfo(name, size, gen, player)
  }
}
