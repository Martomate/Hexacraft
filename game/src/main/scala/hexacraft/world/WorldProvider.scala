package hexacraft.world

import hexacraft.world.coord.{ChunkRelWorld, ColumnRelWorld}

import com.martomate.nbt.Nbt

trait WorldProvider {
  def getWorldInfo: WorldInfo

  def loadChunkData(coords: ChunkRelWorld): Option[Nbt.MapTag]

  def saveChunkData(tag: Nbt.MapTag, coords: ChunkRelWorld): Unit

  def loadColumnData(coords: ColumnRelWorld): Option[Nbt.MapTag]

  def saveColumnData(tag: Nbt.MapTag, coords: ColumnRelWorld): Unit

  def loadWorldData(): Option[Nbt.MapTag]

  def saveWorldData(tag: Nbt.MapTag): Unit
}
