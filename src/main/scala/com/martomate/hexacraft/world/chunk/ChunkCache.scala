package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.world.coord.NeighborOffsets
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.worldlike.BlocksInWorld

import scala.collection.mutable

class ChunkCache(world: BlocksInWorld) {
  private val cache: mutable.Map[Long, Option[IChunk]] = mutable.LongMap.empty
  private var lastChunkCoords: ChunkRelWorld = _
  private var lastChunk: Option[IChunk] = _

  def clearCache(): Unit = cache.clear()

  def getChunk(coords: ChunkRelWorld): Option[IChunk] = cache.getOrElseUpdate(coords.value, world.getChunk(coords))

  def neighbor(side: Int, chunk: IChunk, coords: BlockRelChunk): (BlockRelChunk, Option[IChunk]) = {
    val off = NeighborOffsets(side)
    val i2 = coords.cx + off.dx
    val j2 = coords.cy + off.dy
    val k2 = coords.cz + off.dz
    val c2 = BlockRelChunk(i2, j2, k2)(coords.cylSize)
    if ((i2 & ~15 | j2 & ~15 | k2 & ~15) == 0) {
      (c2, Some(chunk))
    } else {
      val chunkCoords = BlockRelWorld(i2, j2, k2, chunk.coords).getChunkRelWorld
      if (chunkCoords != lastChunkCoords) {
        lastChunkCoords = chunkCoords
        lastChunk = cache.getOrElseUpdate(chunkCoords.value, world.getChunk(chunkCoords))
      }
      (c2, lastChunk)
    }
  }
}
