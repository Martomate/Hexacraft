package hexacraft.world.chunk

import hexacraft.math.bits.Int12
import hexacraft.world.{BlocksInWorld, CollisionDetector, WorldGenerator}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.coord.{BlockRelChunk, BlockRelWorld, ChunkRelWorld, ColumnRelWorld}

import com.martomate.nbt.Nbt

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

trait ChunkColumnTerrain:
  def originalTerrainHeight(cx: Int, cz: Int): Short
  def terrainHeight(cx: Int, cz: Int): Short

object ChunkColumn:
  def create(coords: ColumnRelWorld, worldGenerator: WorldGenerator, columnNBT: Nbt.MapTag): ChunkColumn =
    val generatedHeightMap =
      val gen = worldGenerator.getHeightmapInterpolator(coords)
      for x <- 0 until 16
      yield for z <- 0 until 16
      yield gen(x, z).toShort

    val heightMap = columnNBT.getShortArray("heightMap") match
      case Some(heightNBT) => Array.tabulate(16, 16)((x, z) => heightNBT((x << 4) | z))
      case None            => Array.tabulate(16, 16)((x, z) => generatedHeightMap(x)(z))

    new ChunkColumn(coords, generatedHeightMap, heightMap)

class ChunkColumn private (
    val coords: ColumnRelWorld,
    generatedHeightMap: IndexedSeq[IndexedSeq[Short]],
    heightMap: Array[Array[Short]]
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

  def updateHeightmapAfterBlockUpdate(coords: BlockRelWorld, now: BlockState): Unit =
    val height = terrainHeight(coords.cx, coords.cz)

    if now.blockType != Block.Air then { // a block is being added
      if coords.y > height then heightMap(coords.cx)(coords.cz) = coords.y.toShort
    } else { // a block is being removed
      if coords.y == height then
        // remove and find the next highest

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
    }

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

  def onReloadedResources(): Unit =
    chunks.foreachValue(_.requestRenderUpdate())

  def toNBT: Nbt.MapTag = Nbt.makeMap(
    "heightMap" -> Nbt.ShortArrayTag(ArraySeq.tabulate(16 * 16)(i => heightMap(i >> 4)(i & 0xf)))
  )
