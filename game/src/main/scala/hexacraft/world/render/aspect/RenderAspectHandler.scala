package hexacraft.world.render.aspect

import hexacraft.world.coord.ChunkRelWorld
import hexacraft.world.render.buffer.BufferHandler
import hexacraft.world.render.segment.{ChunkSegmentHandler, ChunkSegs, Segment}

import java.nio.ByteBuffer

class RenderAspectHandler(bufferHandler: BufferHandler[_]) {
  private val segmentHandler: ChunkSegmentHandler = new ChunkSegmentHandler

  private def length: Int = segmentHandler.length

  def fragmentation: Float = segmentHandler.fragmentation

  def render(): Unit = bufferHandler.render(length)

  def setChunkContent(coords: ChunkRelWorld, content: Option[ByteBuffer]): Unit =
    content match
      case Some(data) =>
        if segmentHandler.hasMapping(coords)
        then updateChunk(coords, data)
        else addNewChunk(coords, data)
      case None =>
        removeChunk(coords)

  private def appendData(coords: ChunkRelWorld, data: ByteBuffer): Unit =
    if data.remaining() > 0 then
      val seg = Segment(length, data.remaining())
      bufferHandler.set(seg, data)
      segmentHandler.add(coords, seg)

  private def addNewChunk(coords: ChunkRelWorld, data: ByteBuffer): Unit = appendData(coords, data)

  private def updateChunk(coords: ChunkRelWorld, data: ByteBuffer): Unit =
    val oldLen = segmentHandler.totalLengthForChunk(coords)
    val newLen = data.remaining()

    if oldLen == newLen then
      for s <- segmentHandler.segments(coords)
      do bufferHandler.set(s, data)
    else if oldLen < newLen then
      for s <- segmentHandler.segments(coords)
      do bufferHandler.set(s, data)
      appendData(coords, data)
    else
      val leftOver: ChunkSegs = new ChunkSegs

      for s <- segmentHandler.segments(coords) do
        if data.remaining() >= s.length
        then bufferHandler.set(s, data)
        else leftOver.add(s)

      val splitSeg = leftOver.firstSegment()
      if data.remaining() > 0 then
        val first = Segment(splitSeg.start, data.remaining())
        bufferHandler.set(first, data)
        leftOver.remove(first)

      removeData(coords, leftOver)

  private def removeData(coords: ChunkRelWorld, segments: ChunkSegs): Unit =
    while segments.totalLength > 0 do
      val lastRem = segments.lastSegment()
      val (lastChunk, lastSeg) = segmentHandler.lastSegment().get
      if coords == lastChunk then
        // remove it
        segments.remove(lastRem)
        segmentHandler.remove(coords, lastRem)
      else
        // move data
        val firstRem = segments.firstSegment()
        val len = math.min(firstRem.length, lastSeg.length)

        bufferHandler.copy(lastSeg.start + lastSeg.length - len, firstRem.start, len)

        val destSeg = Segment(firstRem.start, len)
        segments.remove(destSeg)
        segmentHandler.remove(coords, destSeg)

        val srcSeg = Segment(lastSeg.start + lastSeg.length - len, len)
        segmentHandler.remove(lastChunk, srcSeg)
        segmentHandler.add(lastChunk, destSeg)

  private def removeChunk(coords: ChunkRelWorld): Unit =
    val segments = new ChunkSegs
    for s <- segmentHandler.segments(coords) do segments.add(s)
    removeData(coords, segments)

  def unload(): Unit = bufferHandler.unload()
}
