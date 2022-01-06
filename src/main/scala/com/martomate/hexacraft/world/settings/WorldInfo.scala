package com.martomate.hexacraft.world.settings

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}

import java.io.File

class WorldInfo(val worldName: String,
                val worldSize: CylinderSize,
                val gen: WorldGenSettings,
                val planner: CompoundTag,
                val player: CompoundTag)

object WorldInfo {
  def fromNBT(nbtData: CompoundTag, saveDir: File, worldSettings: WorldSettings): WorldInfo = {
    val generalSettings: CompoundTag = NBTUtil.getCompoundTag(nbtData, "general").orNull
    val name: String = NBTUtil.getString(generalSettings, "worldName", worldSettings.name.getOrElse(saveDir.getName))
    val size: CylinderSize = new CylinderSize(NBTUtil.getByte(generalSettings, "worldSize", worldSettings.size.getOrElse(7)))
    val gen: WorldGenSettings = new WorldGenSettings(NBTUtil.getCompoundTag(nbtData, "gen").orNull, worldSettings)
    val planner: CompoundTag = NBTUtil.getCompoundTag(nbtData, "planner").orNull
    val player: CompoundTag = NBTUtil.getCompoundTag(nbtData, "player").orNull

    new WorldInfo(name, size, gen, planner, player)
  }
}
