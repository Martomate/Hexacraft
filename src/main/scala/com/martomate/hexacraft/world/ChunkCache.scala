package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld, NeighborOffsets}

import scala.collection.mutable

class ChunkCache(world: BlocksInWorld) {
  private val cache: mutable.LongMap[Chunk] = mutable.LongMap.empty
  private var lastChunkCoords: ChunkRelWorld = _
  private var lastChunk: Chunk = _

  def clearCache(): Unit = cache.clear()

  def getChunk(coords: ChunkRelWorld): Chunk = {
    if (coords != lastChunkCoords) {
      lastChunkCoords = coords
      lastChunk = cache.getOrNull(coords.value)
      if (lastChunk == null) {
        val newValue = world.getChunk(coords).orNull
        lastChunk = newValue
        cache(coords.value) = newValue
      }
    }
    lastChunk
  }

  def neighbor(side: Int, chunk: Chunk, coords: BlockRelChunk)(implicit cylSize: CylinderSize): (BlockRelChunk, Chunk) = {
    val off = NeighborOffsets(side)
    val i2 = coords.cx + off.dx
    val j2 = coords.cy + off.dy
    val k2 = coords.cz + off.dz
    val c2 = BlockRelChunk(i2, j2, k2)
    if ((i2 & ~15 | j2 & ~15 | k2 & ~15) == 0) {
      (c2, chunk)
    } else {
      val chunkCoords = BlockRelWorld(i2, j2, k2, chunk.coords).getChunkRelWorld
      if (chunkCoords != lastChunkCoords) {
        lastChunkCoords = chunkCoords
        lastChunk = cache.getOrElseUpdate(chunkCoords.value, world.getChunk(chunkCoords).orNull)
      }
      (c2, lastChunk)
    }
  }
}
