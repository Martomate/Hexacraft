package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.util.{Nbt, NBTUtil, RevokeTrackerFn}
import com.martomate.hexacraft.world.{BlocksInWorld, CollisionDetector, WorldProvider}
import com.martomate.hexacraft.world.block.{Blocks, BlockState}
import com.martomate.hexacraft.world.coord.integer.*
import com.martomate.hexacraft.world.gen.WorldGenerator

import com.flowpowered.nbt.{CompoundTag, ShortArrayTag}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

trait ChunkColumnTerrain:
  def originalTerrainHeight(cx: Int, cz: Int): Short
  def terrainHeight(cx: Int, cz: Int): Short

object ChunkColumn:
  def create(coords: ColumnRelWorld, worldGenerator: WorldGenerator, worldProvider: WorldProvider)(using
      Blocks
  ): ChunkColumn =
    val generatedHeightMap =
      val gen = worldGenerator.getHeightmapInterpolator(coords)
      for x <- 0 until 16
      yield for z <- 0 until 16
      yield gen(x, z).toShort

    val columnNBT = worldProvider.loadState(s"data/${coords.value}/column.dat")

    val heightMap = Nbt.from(columnNBT).getShortArray("heightMap") match
      case Some(heightNBT) => Array.tabulate(16, 16)((x, z) => heightNBT((x << 4) | z))
      case None            => Array.tabulate(16, 16)((x, z) => generatedHeightMap(x)(z))

    new ChunkColumn(coords, worldProvider, generatedHeightMap, heightMap)

class ChunkColumn private (
    val coords: ColumnRelWorld,
    worldProvider: WorldProvider,
    generatedHeightMap: IndexedSeq[IndexedSeq[Short]],
    heightMap: Array[Array[Short]]
)(using Blocks: Blocks)
    extends ChunkColumnTerrain:

  private val chunks: mutable.LongMap[Chunk] = mutable.LongMap.empty

  private val chunkEventTrackerRevokeFns = mutable.Map.empty[ChunkRelWorld, RevokeTrackerFn]

  def isEmpty: Boolean = chunks.isEmpty

  private def saveFilePath: String = s"data/${coords.value}/column.dat"

  def originalTerrainHeight(cx: Int, cz: Int): Short = generatedHeightMap(cx)(cz)
  def terrainHeight(cx: Int, cz: Int): Short = heightMap(cx)(cz)

  def getChunk(coords: ChunkRelColumn): Option[Chunk] = chunks.get(coords.value)

  def setChunk(chunk: Chunk): Unit =
    val coords = chunk.coords.getChunkRelColumn
    chunks.put(coords.value, chunk) match
      case Some(oldChunk) =>
        if oldChunk != chunk then
          chunkEventTrackerRevokeFns(oldChunk.coords)()
          chunkEventTrackerRevokeFns += chunk.coords -> chunk.trackEvents(onChunkEvent _)
          onChunkLoaded(chunk)
      case None =>
        chunkEventTrackerRevokeFns += chunk.coords -> chunk.trackEvents(onChunkEvent _)
        onChunkLoaded(chunk)

  def removeChunk(coords: ChunkRelColumn): Option[Chunk] =
    val oldChunkOpt = chunks.remove(coords.value)
    oldChunkOpt match
      case Some(oldChunk) => chunkEventTrackerRevokeFns(oldChunk.coords)()
      case None           =>
    oldChunkOpt

  def allChunks: Iterable[Chunk] = chunks.values

  private def onChunkEvent(event: Chunk.Event): Unit =
    event match
      case Chunk.Event.BlockReplaced(coords, _, now) =>
        onSetBlock(coords, now)
      case _ =>

  def onSetBlock(coords: BlockRelWorld, now: BlockState): Unit =
    val height = terrainHeight(coords.cx, coords.cz)

    if now.blockType != Blocks.Air then { // a block is being added
      if coords.y > height then heightMap(coords.cx)(coords.cz) = coords.y.toShort
    } else { // a block is being removed
      if coords.y == height then
        // remove and find the next highest

        heightMap(coords.cx)(coords.cz) = LazyList
          .range((height - 1).toShort, Short.MinValue, -1.toShort)
          .map(y =>
            getChunk(ChunkRelColumn.create(y >> 4))
              .map(chunk => (y, chunk.getBlock(BlockRelChunk(coords.cx, y & 0xf, coords.cz))))
              .orNull
          )
          .takeWhile(_ != null) // stop searching if the chunk is not loaded
          .collectFirst({ case (y, block) if block.blockType != Blocks.Air => y })
          .getOrElse(Short.MinValue)
    }

  private def onChunkLoaded(chunk: Chunk): Unit =
    val yy = chunk.coords.Y * 16
    for x <- 0 until 16 do
      for z <- 0 until 16 do
        val height = heightMap(x)(z)

        val highestBlockY = (yy + 15 to yy by -1)
          .filter(_ > height)
          .find(y => chunk.getBlock(BlockRelChunk(x, y, z)).blockType != Blocks.Air)

        highestBlockY match
          case Some(h) => heightMap(x)(z) = h.toShort
          case None    =>

  def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit =
    chunks.foreachValue(_.tick(world, collisionDetector))

  def onReloadedResources(): Unit =
    chunks.foreachValue(_.requestRenderUpdate())

  def toNBT: CompoundTag =
    NBTUtil.makeCompoundTag(
      "column",
      Seq(
        new ShortArrayTag("heightMap", Array.tabulate(16 * 16)(i => heightMap(i >> 4)(i & 0xf)))
      )
    )

  def unload(): Unit =
    chunks.foreachValue(_.unload())

    worldProvider.saveState(this.toNBT, saveFilePath)
