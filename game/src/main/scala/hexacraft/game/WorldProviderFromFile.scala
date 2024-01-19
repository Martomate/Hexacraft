package hexacraft.game

import hexacraft.infra.fs.{FileSystem, NbtIO}
import hexacraft.world.{MigrationManager, WorldInfo, WorldProvider, WorldSettings}

import com.martomate.nbt.Nbt

import java.io.File

class WorldProviderFromFile(saveDir: File, worldSettings: WorldSettings, fs: FileSystem) extends WorldProvider {
  new MigrationManager(fs).migrateIfNeeded(saveDir)

  def getWorldInfo: WorldInfo = {
    WorldInfo.fromNBT(loadWorldData().getOrElse(Nbt.emptyMap), saveDir, worldSettings)
  }

  def loadState(path: String): Option[Nbt.MapTag] = {
    NbtIO(fs).loadTag(File(saveDir, path)).map(_._2)
  }

  def saveState(tag: Nbt.MapTag, name: String, path: String): Unit = {
    NbtIO(fs).saveTag(tag, name, File(saveDir, path))
  }
}
