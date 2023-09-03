package hexacraft.world

import hexacraft.world.coord.integer.{ChunkRelWorld, ColumnRelWorld}
import hexacraft.world.settings.WorldInfo

import com.flowpowered.nbt.CompoundTag

trait WorldProvider {
  def getWorldInfo: WorldInfo

  def loadState(path: String): CompoundTag
  def saveState(tag: CompoundTag, path: String): Unit

  final def loadChunkData(coords: ChunkRelWorld): CompoundTag =
    loadState("data/" + coords.getColumnRelWorld.value + "/" + coords.Y.repr.toInt + ".dat")

  final def saveChunkData(tag: CompoundTag, coords: ChunkRelWorld): Unit =
    saveState(tag, "data/" + coords.getColumnRelWorld.value + "/" + coords.Y.repr.toInt + ".dat")

  final def loadColumnData(coords: ColumnRelWorld): CompoundTag =
    loadState(s"data/${coords.value}/column.dat")

  final def saveColumnData(tag: CompoundTag, coords: ColumnRelWorld): Unit =
    saveState(tag, s"data/${coords.value}/column.dat")

  final def loadWorldData(): CompoundTag =
    loadState("world.dat")

  final def saveWorldData(tag: CompoundTag): Unit =
    saveState(tag, "world.dat")
}
