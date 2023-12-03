package hexacraft.world

import com.martomate.nbt.Nbt
import hexacraft.world.coord.integer.{ChunkRelWorld, ColumnRelWorld}
import hexacraft.world.settings.WorldInfo

trait WorldProvider {
  def getWorldInfo: WorldInfo

  def loadState(path: String): Nbt.MapTag
  def saveState(tag: Nbt.MapTag, name: String, path: String): Unit

  final def loadChunkData(coords: ChunkRelWorld): Nbt.MapTag =
    loadState("data/" + coords.getColumnRelWorld.value + "/" + coords.Y.repr.toInt + ".dat")

  final def saveChunkData(tag: Nbt.MapTag, coords: ChunkRelWorld): Unit =
    saveState(tag, "chunk", "data/" + coords.getColumnRelWorld.value + "/" + coords.Y.repr.toInt + ".dat")

  final def loadColumnData(coords: ColumnRelWorld): Nbt.MapTag =
    loadState(s"data/${coords.value}/column.dat")

  final def saveColumnData(tag: Nbt.MapTag, coords: ColumnRelWorld): Unit =
    saveState(tag, "column", s"data/${coords.value}/column.dat")

  final def loadWorldData(): Nbt.MapTag =
    loadState("world.dat")

  final def saveWorldData(tag: Nbt.MapTag): Unit =
    saveState(tag, "world", "world.dat")
}
