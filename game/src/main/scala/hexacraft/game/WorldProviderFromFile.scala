package hexacraft.game

import hexacraft.infra.fs.{FileSystem, NbtIO}
import hexacraft.world.{MigrationManager, WorldInfo, WorldProvider, WorldSettings}
import hexacraft.world.coord.{ChunkRelWorld, ColumnRelWorld}

import com.martomate.nbt.Nbt

import java.io.File

class WorldProviderFromFile(saveDir: File, worldSettings: WorldSettings, fs: FileSystem) extends WorldProvider {
  new MigrationManager(fs).migrateIfNeeded(saveDir)

  def getWorldInfo: WorldInfo = {
    WorldInfo.fromNBT(loadWorldData().getOrElse(Nbt.emptyMap), saveDir, worldSettings)
  }

  private def loadState(path: String): Option[Nbt.MapTag] = {
    NbtIO(fs).loadTag(File(saveDir, path)).map(_._2)
  }

  private def saveState(tag: Nbt.MapTag, name: String, path: String): Unit = {
    NbtIO(fs).saveTag(tag, name, File(saveDir, path))
  }

  override def loadChunkData(coords: ChunkRelWorld): Option[Nbt.MapTag] = {
    loadState("data/" + coords.getColumnRelWorld.value + "/" + coords.Y.repr.toInt + ".dat")
  }

  override def saveChunkData(tag: Nbt.MapTag, coords: ChunkRelWorld): Unit = {
    saveState(tag, "chunk", "data/" + coords.getColumnRelWorld.value + "/" + coords.Y.repr.toInt + ".dat")
  }

  override def loadColumnData(coords: ColumnRelWorld): Option[Nbt.MapTag] = {
    loadState(s"data/${coords.value}/column.dat")
  }

  override def saveColumnData(tag: Nbt.MapTag, coords: ColumnRelWorld): Unit = {
    saveState(tag, "column", s"data/${coords.value}/column.dat")
  }

  override def loadWorldData(): Option[Nbt.MapTag] = {
    loadState("world.dat")
  }

  override def saveWorldData(tag: Nbt.MapTag): Unit = {
    saveState(tag, "world", "world.dat")
  }
}
