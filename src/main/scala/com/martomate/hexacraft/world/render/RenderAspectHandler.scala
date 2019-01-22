package com.martomate.hexacraft.world.render

import java.nio.ByteBuffer

class RenderAspectHandler(bufferHandler: BufferHandler[_]) {
  private val segmentHandler: ChunkSegmentHandler = new ChunkSegmentHandler

  private def length: Int = segmentHandler.length

  def render(): Unit = {
    println(length)
    bufferHandler.render(length)
  }

  def setChunkContent(chunk: ChunkRenderer, content: Option[ByteBuffer]): Unit = content match {
    case Some(data) =>
      if (segmentHandler.hasMapping(chunk)) {
        updateChunk(chunk, data)
      } else {
        addNewChunk(chunk, data)
      }
    case None =>
      removeChunk(chunk)
  }

  private def appendData(chunk: ChunkRenderer, data: ByteBuffer): Unit = {
    if (data.remaining() > 0) {
      val seg = Segment(length, data.remaining())
      bufferHandler.set(seg, data)

      segmentHandler.add(chunk, seg)
    }
  }

  private def addNewChunk(chunk: ChunkRenderer, data: ByteBuffer): Unit = {
    appendData(chunk, data)
  }

  private def updateChunk(chunk: ChunkRenderer, data: ByteBuffer): Unit = {
    val oldLen = segmentHandler.totalLengthForChunk(chunk)
    val newLen = data.remaining()

    if (oldLen == newLen) {
      for (s <- segmentHandler.segments(chunk)) {
        bufferHandler.set(s, data)
      }
    } else if (oldLen < newLen) {
      for (s <- segmentHandler.segments(chunk)) {
        bufferHandler.set(s, data)
      }
      appendData(chunk, data)
    } else {
      val leftOver: ChunkSegs = new ChunkSegs

      for (s <- segmentHandler.segments(chunk)) {
        if (data.remaining() >= s.length)
          bufferHandler.set(s, data)
        else
          leftOver.add(s)
      }

      val splitSeg = leftOver.firstSegment()
      if (data.remaining() > 0) {
        val first = Segment(splitSeg.start, data.remaining())

        bufferHandler.set(first, data)
        leftOver.remove(first)
      }

      removeData(chunk, leftOver)
    }
  }

  private def removeData(chunk: ChunkRenderer, segments: ChunkSegs): Unit = {
    while (segments.totalLength > 0) {
      val lastRem = segments.lastSegment()
      val (lastChunk, lastSeg) = segmentHandler.lastSegment().get
      if (chunk == lastChunk) {
        // remove it
        segments.remove(lastRem)
        segmentHandler.remove(chunk, lastRem)
      } else {
        // move data
        val firstRem = segments.firstSegment()
        val len = math.min(firstRem.length, lastSeg.length)

        bufferHandler.copy(lastSeg.start + lastSeg.length - len, firstRem.start, len)

        val destSeg = Segment(firstRem.start, len)
        segments.remove(destSeg)
        segmentHandler.remove(chunk, destSeg)

        val srcSeg = Segment(lastSeg.start + lastSeg.length - len, len)
        segmentHandler.remove(lastChunk, srcSeg)
        segmentHandler.add(lastChunk, destSeg)
      }
    }
  }

  private def removeChunk(chunk: ChunkRenderer): Unit = {
    val segments = new ChunkSegs
    for (s <- segmentHandler.segments(chunk)) segments.add(s)

    removeData(chunk, segments)
  }

  def unload(): Unit = bufferHandler.unload()
}
