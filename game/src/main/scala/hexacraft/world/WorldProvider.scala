package hexacraft.world

import hexacraft.world.coord.{ChunkRelWorld, ColumnRelWorld}

import com.martomate.nbt.Nbt

import java.util.UUID

trait WorldProvider {
  def getWorldInfo: WorldInfo

  def loadState(path: String): Option[Nbt.MapTag]
  def saveState(tag: Nbt.MapTag, name: String, path: String): Unit

  final def loadChunkData(coords: ChunkRelWorld): Option[Nbt.MapTag] = {
    loadState("data/" + coords.getColumnRelWorld.value + "/" + coords.Y.repr.toInt + ".dat")
  }

  final def saveChunkData(tag: Nbt.MapTag, coords: ChunkRelWorld): Unit = {
    saveState(tag, "chunk", "data/" + coords.getColumnRelWorld.value + "/" + coords.Y.repr.toInt + ".dat")
  }

  final def loadColumnData(coords: ColumnRelWorld): Option[Nbt.MapTag] = {
    loadState(s"data/${coords.value}/column.dat")
  }

  final def saveColumnData(tag: Nbt.MapTag, coords: ColumnRelWorld): Unit = {
    saveState(tag, "column", s"data/${coords.value}/column.dat")
  }

  final def loadWorldData(): Option[Nbt.MapTag] = {
    loadState("world.dat")
  }

  final def saveWorldData(tag: Nbt.MapTag): Unit = {
    saveState(tag, "world", "world.dat")
  }

  final def loadPlayerData(id: UUID): Option[Nbt.MapTag] = {
    loadState(s"players/${id.toString}.dat")
  }

  final def savePlayerData(tag: Nbt.MapTag, id: UUID): Unit = {
    saveState(tag, "", s"players/${id.toString}.dat")
  }
}
