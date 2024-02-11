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

  def setChunkContent(coords: ChunkRelWorld, content: ByteBuffer): Unit = {
    if memorySegments.hasMapping(coords) then {
      updateChunk(coords, content)
    } else {
      addNewChunk(coords, content)
    }
  }

  def clearChunkContent(coords: ChunkRelWorld): Unit = {
    removeChunk(coords)
  }

  private def appendData(coords: ChunkRelWorld, data: ByteBuffer): Unit = {
    val numBytes = data.remaining()
    if numBytes > 0 then {
      val seg = memorySegments.push(coords, numBytes)
      bufferHandler.set(seg, data)
    }
  }

  private def addNewChunk(coords: ChunkRelWorld, data: ByteBuffer): Unit = {
    appendData(coords, data)
  }

  private def updateChunk(coords: ChunkRelWorld, data: ByteBuffer): Unit = {
    val oldLen = memorySegments.totalLengthForChunk(coords)
    val newLen = data.remaining()

    if oldLen == newLen then { // The new data fits perfectly in the allocated space
      // overwrite the old data with the new data
      for s <- memorySegments.segments(coords) do {
        bufferHandler.set(s, data)
      }
    } else if oldLen < newLen then { // The new data takes more space than the old data
      // overwrite the old data with as much of the new data as possible
      for s <- memorySegments.segments(coords) do {
        bufferHandler.set(s, data)
      }
      // append the rest of the data at the end of the buffer
      appendData(coords, data)
    } else { // The new data takes less space than the old data
      val leftOver: SegmentSet = new SegmentSet

      // overwrite the first segments that can be completely filled with the new data
      for s <- memorySegments.segments(coords) do {
        if data.remaining() >= s.length then {
          bufferHandler.set(s, data)
        } else {
          leftOver.add(s)
        }
      }

      // overwrite the rest of the new data into part of an existing segment
      val splitSeg = leftOver.firstSegment()
      if data.remaining() > 0 then {
        val first = Segment(splitSeg.start, data.remaining())
        bufferHandler.set(first, data)
        leftOver.remove(first)
      }

      // remove the space used by the old data but not needed by the new data (by moving data from the end of buffer)
      removeData(coords, leftOver)
    }
  }

  private def removeData(coords: ChunkRelWorld, segments: SegmentSet): Unit = {
    while segments.totalLength > 0 do {
      val lastRem = segments.lastSegment()
      val (lastChunk, lastSeg) = memorySegments.lastSegment.get
      if coords == lastChunk then {
        // remove it
        memorySegments.pop(coords, lastRem) // TODO: this line has crashed multiple times, not sure why
        segments.remove(lastRem)
      } else {
        // move data
        val firstRem = segments.firstSegment()
        val len = math.min(firstRem.length, lastSeg.length)

        val dataSegment = Segment(lastSeg.start + lastSeg.length - len, len)
        val holeSegment = Segment(firstRem.start, len)

        bufferHandler.copy(dataSegment.start, holeSegment.start, len)

        memorySegments.pop(lastChunk, dataSegment)
        memorySegments.relabel(coords, lastChunk, holeSegment)
        segments.remove(holeSegment)
      }
    }
  }

  private def removeChunk(coords: ChunkRelWorld): Unit = {
    val segments = new SegmentSet
    for s <- memorySegments.segments(coords) do {
      segments.add(s)
    }
    removeData(coords, segments)
  }

  def unload(): Unit = {
    bufferHandler.unload()
  }
}
