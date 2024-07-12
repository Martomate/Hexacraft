package hexacraft.client.render

import hexacraft.util.{DenseKeyedSegmentStack, Segment, SegmentSet}
import hexacraft.world.coord.ChunkRelWorld

import java.nio.ByteBuffer

class BlockFaceBatchRenderer(bufferHandler: BufferHandler[?]) {
  private val memorySegments: DenseKeyedSegmentStack[ChunkRelWorld] = new DenseKeyedSegmentStack

  def fragmentation: Float = memorySegments.fragmentation

  def isEmpty: Boolean = memorySegments.lastSegment.isEmpty

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
      val totalEnd = lastSeg.start + lastSeg.length
      val lastHole = segments.lastSegment()
      val lastHoleEnd = lastHole.start + lastHole.length
      val endLength = totalEnd - lastHoleEnd

      if endLength == 0 then {
        segments.remove(lastHole)
        memorySegments.pop(tempCoords, lastHole)
      } else {
        val firstHole = segments.firstSegment()
        val maxCopyLen = math.min(endLength, firstHole.length)

        bufferHandler.copy(totalEnd - maxCopyLen, firstHole.start, maxCopyLen)
        segments.remove(Segment(firstHole.start, maxCopyLen))

        var lengthLeft = maxCopyLen
        while lengthLeft > 0 do {
          val (c, s) = memorySegments.lastSegment.get
          val l = Math.min(s.length, lengthLeft)
          memorySegments.pop(c, Segment(s.start + s.length - l, l))
          memorySegments.relabel(tempCoords, c, Segment(firstHole.start + lengthLeft - l, l))
          lengthLeft -= l
        }
      }
    }
  }

  def unload(): Unit = {
    bufferHandler.unload()
  }
}
