package com.martomate.hexacraft.world.render.segment

import com.martomate.hexacraft.world.render.ChunkRenderer

import scala.collection.mutable

class ChunkSegmentHandler {
  private val contentMap: mutable.Map[ChunkRenderer, ChunkSegs] = mutable.Map.empty
  private val allSegments: ChunkSegsWithKey[ChunkRenderer] = new ChunkSegsWithKey

  def length: Int = lastSegment().map(s => s._2.start + s._2.length).getOrElse(0)

  def hasMapping(chunk: ChunkRenderer): Boolean = contentMap.get(chunk).exists(_.totalLength != 0)

  def totalLengthForChunk(chunk: ChunkRenderer): Int = contentMap.get(chunk).map(_.totalLength).getOrElse(0)

  def add(chunk: ChunkRenderer, segment: Segment): Unit = {
    contentMap.getOrElseUpdate(chunk, new ChunkSegs).add(segment)
    allSegments.add(chunk, segment)
  }

  // TODO: Handle removal of parts of existing segments
  /** segment [a, b] has to either exist as [a, b] or be part of a bigger existing segment [c, d], c <= a, d >= b */
  def remove(chunk: ChunkRenderer, segment: Segment): Unit = {
    allSegments.remove(chunk, segment)
    contentMap.get(chunk).exists(_.remove(segment))
  }

  def segments(chunk: ChunkRenderer): Iterable[Segment] = contentMap.getOrElse(chunk, Iterable.empty)

  def lastSegment(): Option[(ChunkRenderer, Segment)] = if (allSegments.totalLength > 0) Some(allSegments.lastKeyAndSegment()) else None
}
