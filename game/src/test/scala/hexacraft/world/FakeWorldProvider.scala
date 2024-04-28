package hexacraft.world

import hexacraft.world.coord.{ChunkRelWorld, ColumnRelWorld}

import com.martomate.nbt.Nbt

class FakeWorldProvider(seed: Long)(using cylSize: CylinderSize) extends WorldProvider {
  override def getWorldInfo: WorldInfo = new WorldInfo(
    "test world",
    cylSize,
    new WorldGenSettings(seed, 0.1, 0.01, 0.01, 0.001, 0.001),
    Nbt.emptyMap
  )

  private var fs: Map[String, Nbt.MapTag] = Map.empty

  private def loadState(path: String): Option[Nbt.MapTag] = fs.get(path)

  private def saveState(tag: Nbt.MapTag, name: String, path: String): Unit = fs += path -> tag

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
