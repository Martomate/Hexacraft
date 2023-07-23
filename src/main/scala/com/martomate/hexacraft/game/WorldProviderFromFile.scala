package com.martomate.hexacraft.game

import com.martomate.hexacraft.infra.fs.{FileSystem, NbtIO}
import com.martomate.hexacraft.nbt.NBTUtil
import com.martomate.hexacraft.world.{MigrationManager, WorldProvider}
import com.martomate.hexacraft.world.settings.{WorldInfo, WorldSettings}

import com.flowpowered.nbt.CompoundTag
import java.io.File

class WorldProviderFromFile(saveDir: File, worldSettings: WorldSettings, fs: FileSystem) extends WorldProvider {
  new MigrationManager(fs).migrateIfNeeded(saveDir)

  def getWorldInfo: WorldInfo = WorldInfo.fromNBT(loadState("world.dat"), saveDir, worldSettings)

  def loadState(path: String): CompoundTag = {
    new NbtIO(fs).loadTag(new File(saveDir, path))
  }

  def saveState(tag: CompoundTag, path: String): Unit = {
    new NbtIO(fs).saveTag(tag, new File(saveDir, path))
  }
}
