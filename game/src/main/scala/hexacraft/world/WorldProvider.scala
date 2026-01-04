package hexacraft.world

import hexacraft.nbt.Nbt
import hexacraft.world.coord.{ChunkRelWorld, ColumnRelWorld}

import java.util.UUID

trait WorldProvider {
  def getWorldInfo: WorldInfo

  def loadState(path: WorldProvider.Path): Option[Nbt.MapTag]
  def saveState(path: WorldProvider.Path, tag: Nbt.MapTag): Unit

  final def loadChunkData(coords: ChunkRelWorld): Option[Nbt.MapTag] = {
    loadState(WorldProvider.Path.ChunkData(coords))
  }

  final def saveChunkData(tag: Nbt.MapTag, coords: ChunkRelWorld): Unit = {
    saveState(WorldProvider.Path.ChunkData(coords), tag)
  }

  final def loadColumnData(coords: ColumnRelWorld): Option[Nbt.MapTag] = {
    loadState(WorldProvider.Path.ColumnData(coords))
  }

  final def saveColumnData(tag: Nbt.MapTag, coords: ColumnRelWorld): Unit = {
    saveState(WorldProvider.Path.ColumnData(coords), tag)
  }

  final def loadWorldData(): Option[Nbt.MapTag] = {
    loadState(WorldProvider.Path.WorldData)
  }

  final def saveWorldData(tag: Nbt.MapTag): Unit = {
    saveState(WorldProvider.Path.WorldData, tag)
  }

  final def loadPlayerData(id: UUID): Option[Nbt.MapTag] = {
    loadState(WorldProvider.Path.PlayerData(id))
  }

  final def savePlayerData(tag: Nbt.MapTag, id: UUID): Unit = {
    saveState(WorldProvider.Path.PlayerData(id), tag)
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
