package com.martomate.hexacraft.world.column

import com.flowpowered.nbt.ShortArrayTag
import com.martomate.hexacraft.util.NBTUtil
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.chunk.{Chunk, ChunkBlockListener, ChunkEventListener}
import com.martomate.hexacraft.world.collision.CollisionDetector
import com.martomate.hexacraft.world.coord.integer._
import com.martomate.hexacraft.world.gen.WorldGenerator
import com.martomate.hexacraft.world.settings.WorldProvider

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class ChunkColumn(val coords: ColumnRelWorld, worldGenerator: WorldGenerator, worldProvider: WorldProvider) extends ChunkBlockListener with ChunkEventListener {
  private val chunks: mutable.LongMap[Chunk] = mutable.LongMap.empty

  def isEmpty: Boolean = chunks.isEmpty

  private[world] val generatedHeightMap = {
    val interp = worldGenerator.getHeightmapInterpolator(coords)

    for (x <- 0 until 16) yield {
      for (z <- 0 until 16) yield {
        interp(x, z).toShort
      }
    }
  }

  private def saveFilePath: String = s"data/${coords.value}/column.dat"

  private val _heightMap: IndexedSeq[Array[Short]] = {
    val columnNBT = worldProvider.loadState(saveFilePath)
    NBTUtil.getShortArray(columnNBT, "heightMap") match {
      case Some(heightNBT) =>
        for (x <- 0 until 16) yield Array.tabulate(16)(z => heightNBT((x << 4) | z))
      case None =>
        for (x <- 0 until 16) yield Array.tabulate(16)(z => generatedHeightMap(x)(z))
    }
  }

  def heightMap(x: Int, z: Int): Short = {
    _heightMap(x)(z)
  }

  def getChunk(coords: ChunkRelColumn): Option[Chunk] = chunks.get(coords.value)
  def setChunk(chunk: Chunk): Unit = {
    val coords = chunk.coords.getChunkRelColumn
    chunks.put(coords.value, chunk) match {
      case Some(`chunk`) =>
      case oldChunkOpt =>
        handleEventsOnChunkRemoval(oldChunkOpt)

        chunk.addEventListener(this)
        chunk.addBlockEventListener(this)
        onChunkLoaded(chunk)
    }
  }
  def removeChunk(coords: ChunkRelColumn): Option[Chunk] = {
    val oldChunkOpt = chunks.remove(coords.value)
    handleEventsOnChunkRemoval(oldChunkOpt)
    oldChunkOpt
  }
  def allChunks: Iterable[Chunk] = chunks.values

  private def handleEventsOnChunkRemoval(oldChunkOpt: Option[Chunk]): Unit = oldChunkOpt foreach { oldChunk =>
    oldChunk.removeEventListener(this)
    oldChunk.removeBlockEventListener(this)
  }

  def onSetBlock(coords: BlockRelWorld, prev: BlockState, now: BlockState): Unit = {
    val height = heightMap(coords.cx, coords.cz)
    if (now.blockType == Blocks.Air) {
      if (coords.y == height) {
        // remove and find the next highest
        var y: Int = height
        var ch: Option[Chunk] = None
        do {
          y -= 1
          ch = getChunk(ChunkRelColumn.create(y >> 4))
          ch match {
            case Some(chunk) =>
              if (chunk.getBlock(BlockRelChunk(coords.cx, y & 0xf, coords.cz)).blockType != Blocks.Air)
                ch = None
              else
                y -= 1
            case None =>
              y = Short.MinValue
          }//.filter(_.getBlock(BlockRelChunk(coords.cx, y & 0xf, coords.cz, coords.cylSize)).blockType != Block.Air)
        } while (ch.isDefined)

        _heightMap(coords.cx)(coords.cz) = y.toShort
      }
    } else {
      if (coords.y > height) {
        _heightMap(coords.cx)(coords.cz) = coords.y.toShort
      }
    }

    eventListeners.foreach(_.onSetBlock(coords, prev, now))
  }

  private def onChunkLoaded(chunk: Chunk): Unit = {
    val yy = chunk.coords.Y * 16
    for (x <- 0 until 16) {
      for (z <- 0 until 16) {
        val height = heightMap(x, z)
        (yy + 15 to yy by -1).filter(_ > height).find(y =>
          chunk.getBlock(BlockRelChunk(x, y, z)).blockType != Blocks.Air
        ).foreach(h => {
          _heightMap(x)(z) = h.toShort
        })
      }
    }
  }

  def tick(collisionDetector: CollisionDetector): Unit = {
    chunks.foreachValue(_.tick(collisionDetector))
  }

  def onReloadedResources(): Unit = {
    chunks.foreachValue(_.requestRenderUpdate())
  }

  def unload(): Unit = {
    chunks.foreachValue(_.unload())

    worldProvider.saveState(NBTUtil.makeCompoundTag("column", Seq(
      new ShortArrayTag("heightMap", Array.tabulate(16*16)(i => heightMap(i >> 4, i & 0xf)))
    )), saveFilePath)

    eventListeners.clear()
  }

  private val eventListeners: ArrayBuffer[ChunkColumnListener] = ArrayBuffer.empty
  def addEventListener(listener: ChunkColumnListener): Unit = eventListeners += listener
  def removeEventListener(listener: ChunkColumnListener): Unit = eventListeners -= listener

  def onBlockNeedsUpdate(coords: BlockRelWorld): Unit =
    eventListeners.foreach(_.onBlockNeedsUpdate(coords))

  def onChunkNeedsRenderUpdate(coords: ChunkRelWorld): Unit =
    eventListeners.foreach(_.onChunkNeedsRenderUpdate(coords))

  def onChunksNeighborNeedsRenderUpdate(coords: ChunkRelWorld, side: Int): Unit =
    eventListeners.foreach(_.onChunksNeighborNeedsRenderUpdate(coords, side))
}
