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

    if oldLen == newLen then {
      for s <- memorySegments.segments(coords) do {
        bufferHandler.set(s, data)
      }
    } else if oldLen < newLen then {
      for s <- memorySegments.segments(coords) do {
        bufferHandler.set(s, data)
      }
      appendData(coords, data)
    } else {
      val leftOver: SegmentSet = new SegmentSet

      for s <- memorySegments.segments(coords) do {
        if data.remaining() >= s.length then {
          bufferHandler.set(s, data)
        } else {
          leftOver.add(s)
        }
      }

      val splitSeg = leftOver.firstSegment()
      if data.remaining() > 0 then {
        val first = Segment(splitSeg.start, data.remaining())
        bufferHandler.set(first, data)
        leftOver.remove(first)
      }

      removeData(coords, leftOver)
    }
  }

  private def removeData(coords: ChunkRelWorld, segments: SegmentSet): Unit = {
    while segments.totalLength > 0 do {
      val lastRem = segments.lastSegment()
      val (lastChunk, lastSeg) = memorySegments.lastSegment.get
      if coords == lastChunk then {
        // remove it
        memorySegments.pop(coords, lastRem) // TODO: this line crashed one time, not sure why
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
