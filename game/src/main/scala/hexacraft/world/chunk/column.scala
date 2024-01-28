package hexacraft.world.chunk

import hexacraft.math.noise.Data2D
import hexacraft.world.coord.ColumnRelWorld

import com.martomate.nbt.Nbt

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

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
        col.heightMap.map(heightMap => Nbt.ShortArrayTag(ArraySeq.tabulate(16 * 16)(i => heightMap(i >> 4)(i & 0xf))))
      )
    }
  }
}

type ChunkColumnHeightMap = Array[Array[Short]]

object ChunkColumnHeightMap {

  /** @param f a function from (x, z) => height */
  def from(f: (Int, Int) => Short): ChunkColumnHeightMap = Array.tabulate(16, 16)(f)

  def fromData2D(data: Data2D): ChunkColumnHeightMap = from((x, z) => data(x, z).toShort)
}

trait ChunkColumnTerrain {
  def originalTerrainHeight(cx: Int, cz: Int): Short

  def terrainHeight(cx: Int, cz: Int): Short
}

object ChunkColumn {
  def create(
      coords: ColumnRelWorld,
      generatedHeightMap: ChunkColumnHeightMap,
      columnData: Option[ChunkColumnData]
  ): ChunkColumn = {
    val heightMap = columnData
      .flatMap(_.heightMap)
      .getOrElse(ChunkColumnHeightMap.from((x, z) => generatedHeightMap(x)(z)))

    new ChunkColumn(coords, generatedHeightMap, heightMap)
  }
}

class ChunkColumn private (
    val coords: ColumnRelWorld,
    generatedHeightMap: ChunkColumnHeightMap,
    val heightMap: ChunkColumnHeightMap
) extends ChunkColumnTerrain {

  val chunks: mutable.LongMap[Chunk] = mutable.LongMap.empty

  def originalTerrainHeight(cx: Int, cz: Int): Short = generatedHeightMap(cx)(cz)

  def terrainHeight(cx: Int, cz: Int): Short = heightMap(cx)(cz)

  def toNBT: Nbt.MapTag = ChunkColumnData(Some(heightMap)).toNBT
}
