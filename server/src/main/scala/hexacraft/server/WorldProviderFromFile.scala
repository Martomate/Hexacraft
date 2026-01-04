package hexacraft.server

import hexacraft.infra.fs.{FileSystem, NbtFile}
import hexacraft.nbt.Nbt
import hexacraft.server.world.MigrationManager
import hexacraft.world.{WorldInfo, WorldProvider, WorldSettings}
import hexacraft.world.coord.{ChunkRelWorld, ColumnRelWorld}

import java.io.File
import java.util.UUID

class WorldProviderFromFile(saveDir: File, worldSettings: WorldSettings, fs: FileSystem) extends WorldProvider {
  new MigrationManager(fs).migrateIfNeeded(saveDir)

  def getWorldInfo: WorldInfo = {
    WorldInfo.fromNBT(loadWorldData().getOrElse(Nbt.emptyMap), saveDir, worldSettings)
  }

  def loadState(path: WorldProvider.Path): Option[Nbt.MapTag] = {
    val file = NbtFile(File(saveDir, resolvePath(path)), fs)
    Option.when(file.exists)(file.readMapTag._2)
  }

  def saveState(path: WorldProvider.Path, tag: Nbt.MapTag): Unit = {
    val file = NbtFile(File(saveDir, resolvePath(path)), fs)
    file.writeMapTag(tag, "")
  }

  private def resolvePath(path: WorldProvider.Path): String = {
    import WorldProvider.Path
    path match {
      case Path.ChunkData(coords: ChunkRelWorld) =>
        s"data/${coords.getColumnRelWorld.value}/${coords.Y.repr.toInt}.dat"
      case Path.ColumnData(coords: ColumnRelWorld) =>
        s"data/${coords.value}/column.dat"
      case Path.PlayerData(id: UUID) =>
        s"players/${id.toString}.dat"
      case Path.WorldData =>
        "world.dat"
    }
  }
}
