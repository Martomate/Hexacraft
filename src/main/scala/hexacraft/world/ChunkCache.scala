package hexacraft.world

import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.integer.ChunkRelWorld

import scala.collection.mutable

class ChunkCache(world: BlocksInWorld) {
  private val cache: mutable.LongMap[Chunk] = mutable.LongMap.empty
  private var lastChunkCoords: Option[ChunkRelWorld] = None
  private var lastChunk: Chunk = _

  def clearCache(): Unit = {
    cache.clear()
    lastChunkCoords = None
    lastChunk = null
  }

  def getChunk(coords: ChunkRelWorld): Chunk = {
    if (lastChunkCoords.isEmpty || coords != lastChunkCoords.get) {
      lastChunkCoords = Some(coords)
      lastChunk = cache.getOrNull(coords.value)
      if (lastChunk == null) {
        val newValue = world.getChunk(coords).orNull
        lastChunk = newValue
        cache(coords.value) = newValue
      }
    }
    lastChunk
  }
}
