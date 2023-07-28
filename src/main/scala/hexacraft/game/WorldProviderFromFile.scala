package hexacraft.game

import hexacraft.infra.fs.{FileSystem, NbtIO}
import hexacraft.nbt.NBTUtil
import hexacraft.world.{MigrationManager, WorldProvider}
import hexacraft.world.settings.{WorldInfo, WorldSettings}

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
