package com.martomate.hexacraft.world.settings

import com.flowpowered.nbt.CompoundTag
import com.martomate.hexacraft.util.NBTUtil
import com.martomate.hexacraft.world.MigrationManager

import java.io.File

class WorldProviderFromFile(saveDir: File, worldSettings: WorldSettings) extends WorldProvider {
  MigrationManager.migrateIfNeeded(saveDir)

  def getWorldInfo: WorldInfo = WorldInfo.fromNBT(loadState("world.dat"), saveDir, worldSettings)

  def loadState(path: String): CompoundTag = {
    NBTUtil.loadTag(new File(saveDir, path))
  }

  def saveState(tag: CompoundTag, path: String): Unit = {
    NBTUtil.saveTag(tag, new File(saveDir, path))
  }
}
