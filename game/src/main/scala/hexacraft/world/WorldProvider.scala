package hexacraft.world

import hexacraft.nbt.Nbt
import hexacraft.world.coord.{ChunkRelWorld, ColumnRelWorld}

import java.util.UUID

trait WorldProvider {
  def getWorldInfo: WorldInfo

  def loadState(path: WorldProvider.Path): Option[Nbt.MapTag]
  def saveState(tag: Nbt.MapTag, name: String, path: WorldProvider.Path): Unit

  final def loadChunkData(coords: ChunkRelWorld): Option[Nbt.MapTag] = {
    loadState(WorldProvider.Path.ChunkData(coords))
  }

  final def saveChunkData(tag: Nbt.MapTag, coords: ChunkRelWorld): Unit = {
    saveState(tag, "chunk", WorldProvider.Path.ChunkData(coords))
  }

  final def loadColumnData(coords: ColumnRelWorld): Option[Nbt.MapTag] = {
    loadState(WorldProvider.Path.ColumnData(coords))
  }

  final def saveColumnData(tag: Nbt.MapTag, coords: ColumnRelWorld): Unit = {
    saveState(tag, "column", WorldProvider.Path.ColumnData(coords))
  }

  final def loadWorldData(): Option[Nbt.MapTag] = {
    loadState(WorldProvider.Path.WorldData)
  }

  final def saveWorldData(tag: Nbt.MapTag): Unit = {
    saveState(tag, "world", WorldProvider.Path.WorldData)
  }

  final def loadPlayerData(id: UUID): Option[Nbt.MapTag] = {
    loadState(WorldProvider.Path.PlayerData(id))
  }

  final def savePlayerData(tag: Nbt.MapTag, id: UUID): Unit = {
    saveState(tag, "", WorldProvider.Path.PlayerData(id))
  }
}

object WorldProvider {
  enum Path {
    case ChunkData(coords: ChunkRelWorld)
    case ColumnData(coords: ColumnRelWorld)
    case PlayerData(id: UUID)
    case WorldData
  }
}
