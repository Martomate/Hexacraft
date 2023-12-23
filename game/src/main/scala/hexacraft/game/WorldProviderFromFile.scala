package hexacraft.game

import com.martomate.nbt.Nbt
import hexacraft.infra.fs.{FileSystem, NbtIO}
import hexacraft.world.{MigrationManager, WorldInfo, WorldProvider, WorldSettings}

import java.io.File

class WorldProviderFromFile(saveDir: File, worldSettings: WorldSettings, fs: FileSystem) extends WorldProvider {
  new MigrationManager(fs).migrateIfNeeded(saveDir)

  def getWorldInfo: WorldInfo =
    WorldInfo.fromNBT(loadState("world.dat"), saveDir, worldSettings)

  def loadState(path: String): Nbt.MapTag =
    NbtIO(fs).loadTag(File(saveDir, path))._2

  def saveState(tag: Nbt.MapTag, name: String, path: String): Unit =
    NbtIO(fs).saveTag(tag, name, File(saveDir, path))
}
