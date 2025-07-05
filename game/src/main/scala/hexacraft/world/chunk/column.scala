package hexacraft.world.chunk

import hexacraft.math.noise.Data2D
import hexacraft.nbt.Nbt
import hexacraft.world.WorldGenerator
import hexacraft.world.coord.ColumnRelWorld

import scala.collection.immutable.ArraySeq

class ChunkColumnData(val heightMap: Option[ChunkColumnHeightMap])

object ChunkColumnData {
  def fromNbt(tag: Nbt.MapTag): ChunkColumnData = {
    ChunkColumnData(
      tag
        .getShortArray("heightMap")
        .map(heightNbt => ChunkColumnHeightMap.from((x, z) => heightNbt((x << 4) | z)))
    )
  }

  extension (col: ChunkColumnData) {
    def toNBT: Nbt.MapTag = {
      Nbt.emptyMap.withOptionalField(
        "heightMap",
        col.heightMap.map(heightMap =>
          Nbt.ShortArrayTag.of(Array.tabulate(16 * 16)(i => heightMap.getHeight(i >> 4, i & 0xf)))
        )
      )
    }
  }
}

class ChunkColumnHeightMap(values: Array[Short]) {
  def getHeight(x: Int, z: Int): Short = {
    values((x << 4) | z)
  }

  def setHeight(x: Int, z: Int, height: Short): Unit = {
    values((x << 4) | z) = height
  }
}

object ChunkColumnHeightMap {

  /** @param f a function from (x, z) => height */
  def from(f: (Int, Int) => Short): ChunkColumnHeightMap = ChunkColumnHeightMap(
    Array.tabulate(16 * 16)(i => f(i >> 4, i & 15))
  )

  def fromData2D(data: Data2D): ChunkColumnHeightMap = from((x, z) => data(x, z).toShort)
}

class ChunkColumnTerrain(
    // regenerated on load
    val originalTerrainHeight: ChunkColumnHeightMap,
    val humidity: Data2D,
    val temperature: Data2D,
    // stored on disk
    val terrainHeight: ChunkColumnHeightMap
) {
  def isDesert(cx: Int, cz: Int): Boolean = {
    // TODO: use better units
    humidity(cx, cz) < 0.0 && temperature(cx, cz) > 0.0
  }
}

object ChunkColumnTerrain {
  def create(
      coords: ColumnRelWorld,
      worldGenerator: WorldGenerator,
      columnData: Option[ChunkColumnData]
  ): ChunkColumnTerrain = {
    val generatedHeightMap = ChunkColumnHeightMap.fromData2D(worldGenerator.getHeightmapInterpolator(coords))
    val humidity = worldGenerator.getHumidityForColumn(coords)
    val temperature = worldGenerator.getTemperatureForColumn(coords)

    val heightMap = columnData
      .flatMap(_.heightMap)
      .getOrElse(ChunkColumnHeightMap.from((x, z) => generatedHeightMap.getHeight(x, z)))

    new ChunkColumnTerrain(generatedHeightMap, humidity, temperature, heightMap)
  }
}
