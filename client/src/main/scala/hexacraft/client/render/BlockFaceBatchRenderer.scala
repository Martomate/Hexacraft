package hexacraft.client.render

import hexacraft.util.KeyedSegment
import hexacraft.world.coord.ChunkRelWorld

import java.nio.ByteBuffer

class BlockFaceBatchRenderer(bufferHandler: BufferHandler[?]) {
  private val memorySegments: KeyedSegment[ChunkRelWorld] = new KeyedSegment[ChunkRelWorld]

  def fragmentation: Float = memorySegments.numKeyedSegments.toFloat / memorySegments.numKeys

  def isEmpty: Boolean = memorySegments.isEmpty

  def render(): Unit = {
    bufferHandler.render(memorySegments.usedSegments)
  }

  def update(
      chunksToClear: Seq[ChunkRelWorld],
      chunksToUpdate: Seq[(ChunkRelWorld, ByteBuffer)]
  ): Unit = {
    // Step 1: mark old data as unused
    for coords <- chunksToClear do {
      memorySegments.clear(coords)
    }
    for (coords, _) <- chunksToUpdate do {
      memorySegments.clear(coords)
    }

    // Step 2: fill data into the space of the old data and push the rest at the end
    for (coords, data) <- chunksToUpdate do {
      val segments = memorySegments.allocate(coords, data.remaining())
      for s <- segments do {
        bufferHandler.set(s, data)
      }
    }
  }

  def unload(): Unit = {
    bufferHandler.unload()
  }
}
