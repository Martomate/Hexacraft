package hexacraft.world.chunk

import hexacraft.math.noise.Data2D
import hexacraft.nbt.{Nbt, NbtDecoder, NbtEncoder}
import hexacraft.util.Loop
import hexacraft.world.CylinderSize
import hexacraft.world.block.Block
import hexacraft.world.coord.{BlockRelChunk, BlockRelWorld}

import scala.collection.immutable.ArraySeq

class ChunkColumnData(val heightMap: Option[ChunkColumnHeightMap])

object ChunkColumnData {
  given NbtDecoder[ChunkColumnData] with {
    override def decode(tag: Nbt.MapTag): Option[ChunkColumnData] = {
      Some(
        ChunkColumnData(
          tag
            .getShortArray("heightMap")
            .map(heightNbt => ChunkColumnHeightMap.from((x, z) => heightNbt((x << 4) | z)))
        )
      )
    }
  }

  given NbtEncoder[ChunkColumnData] with {
    override def encode(col: ChunkColumnData): Nbt.MapTag = {
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

  def recalculate(coords: BlockRelWorld, chunks: Int => Option[Chunk])(using
      CylinderSize
  ): Unit = {
    this.setHeight(coords.cx, coords.cz, findHeightFrom(coords, chunks))
  }

  private def findHeightFrom(coords: BlockRelWorld, chunks: Int => Option[Chunk]): Short = {
    val cx = coords.cx
    val cz = coords.cz

    Loop.downTo(coords.y >> 4, Short.MinValue >> 4) { Y =>
      chunks(Y) match {
        case Some(chunk) =>
          Loop.downTo(15, 0) { cy =>
            val block = chunk.getBlock(BlockRelChunk(cx, cy, cz))

            if block.blockType != Block.Air then {
              return ((Y << 4) | cy).toShort
            }
          }
        case None =>
          return Short.MinValue // stop searching if the chunk is not loaded
      }
    }

    Short.MinValue
  }
}

object ChunkColumnHeightMap {

  /** @param f a function from (x, z) => height */
  inline def from(inline f: (Int, Int) => Short): ChunkColumnHeightMap = ChunkColumnHeightMap(
    Array.tabulate(16 * 16)(i => f(i >> 4, i & 15))
  )

  def fromData2D(data: Data2D): ChunkColumnHeightMap = from((x, z) => data(x, z).toShort)
}

class ChunkColumnTerrain(
    val originalTerrainHeight: ChunkColumnHeightMap,
    val terrainHeight: ChunkColumnHeightMap
)

object ChunkColumnTerrain {
  def create(
      generatedHeightMap: ChunkColumnHeightMap,
      columnData: Option[ChunkColumnData]
  ): ChunkColumnTerrain = {
    val heightMap = columnData
      .flatMap(_.heightMap)
      .getOrElse(ChunkColumnHeightMap.from((x, z) => generatedHeightMap.getHeight(x, z)))

    new ChunkColumnTerrain(generatedHeightMap, heightMap)
  }
}
