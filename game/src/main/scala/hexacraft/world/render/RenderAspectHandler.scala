package hexacraft.world.render

import hexacraft.util.{DenseKeyedSegmentStack, Segment, SegmentSet}
import hexacraft.world.coord.ChunkRelWorld

import java.nio.ByteBuffer

class RenderAspectHandler(bufferHandler: BufferHandler[_]) {
  private val memorySegments: DenseKeyedSegmentStack[ChunkRelWorld] = new DenseKeyedSegmentStack

  def fragmentation: Float = memorySegments.fragmentation

  def render(): Unit = {
    bufferHandler.render(memorySegments.length)
  }

  def update(
      chunksToClear: Seq[ChunkRelWorld],
      chunksToUpdate: Seq[(ChunkRelWorld, ByteBuffer)]
  ): Unit = {
    val tempCoords = ChunkRelWorld(-1L) // TODO: make non-temp solution

    // Step 1: move old data to temporary label
    for coords <- chunksToClear do {
      for s <- memorySegments.segments(coords) do {
        memorySegments.relabel(coords, tempCoords, s)
      }
    }
    for (coords, _) <- chunksToUpdate do {
      for s <- memorySegments.segments(coords) do {
        memorySegments.relabel(coords, tempCoords, s)
      }
    }

    // Step 2: fill data into the space of the old data and push the rest at the end
    for (coords, data) <- chunksToUpdate do {
      val (dest, _) = memorySegments.segments(tempCoords).cut(data.remaining())
      for s <- dest do {
        memorySegments.relabel(tempCoords, coords, s)
        bufferHandler.set(s, data)
      }
      val remaining = data.remaining()
      if remaining > 0 then {
        val s = memorySegments.push(coords, remaining)
        bufferHandler.set(s, data)
      }
    }

    // Step 3: fill gap if needed by moving data from the end
    val segments = memorySegments.segments(tempCoords)
    while segments.totalLength > 0 do {
      val (lastChunk, lastSeg) = memorySegments.lastSegment.get

      if tempCoords == lastChunk then {
        val lastRem = segments.lastSegment()
        memorySegments.pop(tempCoords, lastRem)
        segments.remove(lastRem)
      } else {
        val firstRem = segments.firstSegment()
        val len = math.min(firstRem.length, lastSeg.length)

        val dataSegment = Segment(lastSeg.start + lastSeg.length - len, len)
        val holeSegment = Segment(firstRem.start, len)

        // TODO: merge segments from different chunks at the end of the buffers so more can be moved at once
        bufferHandler.copy(dataSegment.start, holeSegment.start, len)

        memorySegments.pop(lastChunk, dataSegment)
        memorySegments.relabel(tempCoords, lastChunk, holeSegment)
        segments.remove(holeSegment)
      }
    }
  }

  def unload(): Unit = {
    bufferHandler.unload()
  }
}
