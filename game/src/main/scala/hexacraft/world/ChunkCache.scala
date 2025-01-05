package hexacraft.world

import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.ChunkRelWorld

import scala.collection.mutable

class ChunkCache(world: BlocksInWorld) {
  private val cache: mutable.LongMap[Chunk] = mutable.LongMap.empty

  private var hasLastChunk: Boolean = false
  private var lastChunkCoords: ChunkRelWorld = ChunkRelWorld(0)
  private var lastChunk: Chunk = null.asInstanceOf[Chunk]

  def clearCache(): Unit = {
    cache.clear()

    hasLastChunk = false
    lastChunkCoords = ChunkRelWorld(0)
    lastChunk = null
  }

  def getChunk(coords: ChunkRelWorld): Chunk = {
    if !hasLastChunk || coords != lastChunkCoords then {
      hasLastChunk = true
      lastChunkCoords = coords
      lastChunk = cache.getOrNull(coords.value)

      if lastChunk == null then {
        val newValue = world.getChunk(coords).orNull
        lastChunk = newValue
        cache(coords.value) = newValue
      }
    }
    lastChunk
  }
}
