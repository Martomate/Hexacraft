package hexacraft.world.render.segment

import hexacraft.world.coord.ChunkRelWorld
import scala.collection.mutable

class ChunkSegmentHandler {
  private val contentMap: mutable.Map[ChunkRelWorld, ChunkSegs] = mutable.Map.empty
  private val allSegments: ChunkSegsWithKey[ChunkRelWorld] = new ChunkSegsWithKey

  def length: Int = lastSegment().map(s => s._2.start + s._2.length).getOrElse(0)

  def fragmentation: Float = allSegments.segmentCount.toFloat / allSegments.keyCount

  def hasMapping(coords: ChunkRelWorld): Boolean =
    contentMap.get(coords).exists(_.totalLength != 0)

  def totalLengthForChunk(coords: ChunkRelWorld): Int =
    contentMap.get(coords).map(_.totalLength).getOrElse(0)

  def add(coords: ChunkRelWorld, segment: Segment): Unit = {
    contentMap.getOrElseUpdate(coords, new ChunkSegs).add(segment)
    allSegments.add(coords, segment)
  }

  // TODO: Handle removal of parts of existing segments
  /** segment [a, b] has to either exist as [a, b] or be part of a bigger existing segment [c, d], c
    * <= a, d >= b
    */
  def remove(coords: ChunkRelWorld, segment: Segment): Unit = {
    allSegments.remove(coords, segment)
    contentMap.get(coords).exists(_.remove(segment))
  }

  def segments(coords: ChunkRelWorld): Iterable[Segment] =
    contentMap.getOrElse(coords, Iterable.empty)

  def lastSegment(): Option[(ChunkRelWorld, Segment)] =
    if (allSegments.totalLength > 0) Some(allSegments.lastKeyAndSegment()) else None
}
