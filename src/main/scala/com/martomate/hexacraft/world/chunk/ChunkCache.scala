package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld, NeighborOffsets}
import com.martomate.hexacraft.world.worldlike.BlocksInWorld

import scala.collection.mutable

class ChunkCache(world: BlocksInWorld) {
  private val cache: mutable.LongMap[IChunk] = mutable.LongMap.empty
  private var lastChunkCoords: ChunkRelWorld = _
  private var lastChunk: IChunk = _

  def clearCache(): Unit = cache.clear()

  def getChunk(coords: ChunkRelWorld): IChunk = {
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

  def neighbor(side: Int, chunk: IChunk, coords: BlockRelChunk)(implicit cylSize: CylinderSize): (BlockRelChunk, IChunk) = {
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
