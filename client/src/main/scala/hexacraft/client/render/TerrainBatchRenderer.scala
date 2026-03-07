package hexacraft.client.render

import hexacraft.util.{KeyedSegment, Loop}
import hexacraft.world.coord.ChunkRelWorld

import java.nio.ByteBuffer

class TerrainBatchRenderer(bufferHandler: BufferHandler[?]) {
  private val memorySegments: KeyedSegment[ChunkRelWorld] = new KeyedSegment[ChunkRelWorld]

  def fragmentation: Float = memorySegments.numKeyedSegments.toFloat / memorySegments.numKeys

  def isEmpty: Boolean = memorySegments.isEmpty

  def render(): Unit = {
    bufferHandler.render(memorySegments.usedSegments)
  }

  def update(
      chunksToClear: IndexedSeq[ChunkRelWorld],
      chunksToUpdate: IndexedSeq[(ChunkRelWorld, ByteBuffer)]
  ): Unit = {
    // Step 1: mark old data as unused
    Loop.array(chunksToClear) { coords =>
      memorySegments.clear(coords)
    }
    Loop.array(chunksToUpdate) { case (coords, _) =>
      memorySegments.clear(coords)
    }

    // Step 2: fill data into the space of the old data and push the rest at the end
    Loop.array(chunksToUpdate) { case (coords, data) =>
      val segments = memorySegments.allocate(coords, data.remaining())
      Loop.iterate(segments.iterator) { s =>
        bufferHandler.set(s, data)
      }
    }
  }

  def unload(): Unit = {
    bufferHandler.unload()
  }
}
