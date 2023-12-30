package hexacraft.world.chunk

import hexacraft.math.bits.Int12
import hexacraft.math.noise.Data2D
import hexacraft.world.{BlocksInWorld, CollisionDetector, CylinderSize}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.coord.{BlockRelChunk, BlockRelWorld, ChunkRelWorld, ColumnRelWorld}

import com.martomate.nbt.Nbt

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

class ChunkColumnData(val heightMap: Option[ChunkColumnHeightMap])

object ChunkColumnData {
  def fromNbt(tag: Nbt.MapTag): ChunkColumnData =
    ChunkColumnData(
      tag
        .getShortArray("heightMap")
        .map(heightNbt => ChunkColumnHeightMap.from((x, z) => heightNbt((x << 4) | z)))
    )

  extension (col: ChunkColumnData)
    def toNBT: Nbt.MapTag =
      Nbt.emptyMap.withOptionalField(
        "heightMap",
        col.heightMap.map(heightMap => Nbt.ShortArrayTag(ArraySeq.tabulate(16 * 16)(i => heightMap(i >> 4)(i & 0xf))))
      )
}

type ChunkColumnHeightMap = Array[Array[Short]]

object ChunkColumnHeightMap {

  /** @param f a function from (x, z) => height */
  def from(f: (Int, Int) => Short): ChunkColumnHeightMap = Array.tabulate(16, 16)(f)

  def fromData2D(data: Data2D): ChunkColumnHeightMap = from((x, z) => data(x, z).toShort)
}

trait ChunkColumnTerrain:
  def originalTerrainHeight(cx: Int, cz: Int): Short
  def terrainHeight(cx: Int, cz: Int): Short

object ChunkColumn:
  def create(
      coords: ColumnRelWorld,
      generatedHeightMap: ChunkColumnHeightMap,
      columnData: Option[ChunkColumnData]
  ): ChunkColumn =
    val heightMap = columnData
      .flatMap(_.heightMap)
      .getOrElse(ChunkColumnHeightMap.from((x, z) => generatedHeightMap(x)(z)))

    new ChunkColumn(coords, generatedHeightMap, heightMap)

class ChunkColumn private (
    val coords: ColumnRelWorld,
    generatedHeightMap: ChunkColumnHeightMap,
    heightMap: ChunkColumnHeightMap
) extends ChunkColumnTerrain:

  private val chunks: mutable.LongMap[Chunk] = mutable.LongMap.empty

  def isEmpty: Boolean = chunks.isEmpty

  def originalTerrainHeight(cx: Int, cz: Int): Short = generatedHeightMap(cx)(cz)
  def terrainHeight(cx: Int, cz: Int): Short = heightMap(cx)(cz)

  def getChunk(Y: Int12): Option[Chunk] = chunks.get(Y.repr.toInt)

  def setChunk(chunk: Chunk): Unit =
    val chunkCoords = chunk.coords

    chunks.put(chunkCoords.Y.repr.toInt, chunk) match
      case Some(`chunk`)  => // the chunk is not new so nothing needs to be done
      case Some(oldChunk) => onChunkLoaded(chunk)
      case None           => onChunkLoaded(chunk)

  def removeChunk(Y: Int12): Option[Chunk] = chunks.remove(Y.repr.toInt)

  def allChunks: Iterable[Chunk] = chunks.values

  def updateHeightmapAfterChunkUpdate(chunk: Chunk)(using CylinderSize): Unit =
    val chunkCoords = chunk.coords
    for
      cx <- 0 until 16
      cz <- 0 until 16
    do
      val blockCoords = BlockRelChunk(cx, 15, cz)
      updateHeightmapAfterBlockUpdate(BlockRelWorld.fromChunk(blockCoords, chunkCoords), chunk.getBlock(blockCoords))

  def updateHeightmapAfterBlockUpdate(coords: BlockRelWorld, now: BlockState): Unit =
    val height = terrainHeight(coords.cx, coords.cz)

    if coords.y >= height then
      if now.blockType != Block.Air then heightMap(coords.cx)(coords.cz) = coords.y.toShort
      else
        heightMap(coords.cx)(coords.cz) = LazyList
          .range((height - 1).toShort, Short.MinValue, -1.toShort)
          .map(y =>
            getChunk(Int12.truncate(y >> 4))
              .map(chunk => (y, chunk.getBlock(BlockRelChunk(coords.cx, y & 0xf, coords.cz))))
              .orNull
          )
          .takeWhile(_ != null) // stop searching if the chunk is not loaded
          .collectFirst({ case (y, block) if block.blockType != Block.Air => y })
          .getOrElse(Short.MinValue)

  private def onChunkLoaded(chunk: Chunk): Unit =
    val yy = chunk.coords.Y.toInt * 16
    for x <- 0 until 16 do
      for z <- 0 until 16 do
        val height = heightMap(x)(z)

        val highestBlockY = (yy + 15 to yy by -1)
          .filter(_ > height)
          .find(y => chunk.getBlock(BlockRelChunk(x, y, z)).blockType != Block.Air)

        highestBlockY match
          case Some(h) => heightMap(x)(z) = h.toShort
          case None    =>

  def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit =
    chunks.foreachValue(_.tick(world, collisionDetector))

  def toNBT: Nbt.MapTag = ChunkColumnData(Some(heightMap)).toNBT
