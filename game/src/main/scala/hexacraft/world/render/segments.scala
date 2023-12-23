package hexacraft.world.render

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

case class Segment(start: Int, length: Int) {
  require(start >= 0, "'start' has to be non-negative")
  require(length > 0, "'length' has to be positive")

  def contains(inner: Segment): Boolean =
    inner.start >= start && inner.start + inner.length <= start + length

  def overlaps(other: Segment): Boolean =
    other.start < start + length && other.start + other.length > start
}

class ChunkSegs extends mutable.Iterable[Segment] {

  private val segments: mutable.TreeSet[Segment] = mutable.TreeSet.empty { (s1, s2) =>
    if (s1.overlaps(s2)) 0
    else s1.start - s2.start
  }
  private val segmentsContain: mutable.TreeMap[Segment, Segment] = mutable.TreeMap.empty { (s1, s2) =>
    if (s2.contains(s1)) 0
    else s1.start - s2.start
  }
  private var _totalLength = 0

  protected def _add(seg: Segment): Unit = {
    require(segments.add(seg), s"$seg cannot be added")
    segmentsContain(seg) = seg
    _totalLength += seg.length
  }
  protected def _remove(seg: Segment): Unit = {
    require(segmentsContain.remove(seg).isDefined, s"$seg cannot be removed")
    segments.remove(seg)
    _totalLength -= seg.length
  }

  def add(seg: Segment): Unit = {
    require(!segments.contains(seg))
    _add(seg)
  }

  /** segment [a, b] has to either exist as [a, b] or be part of a bigger existing segment [c, d], c
    * <= a, d >= b
    */
  def remove(seg: Segment): Boolean = {
    containedInSegments(seg) match {
      case Some(other) =>
        val len1 = seg.start - other.start
        val len2 = other.length - len1 - seg.length

        _remove(other)
        if (len1 > 0) _add(Segment(other.start, len1))
        if (len2 > 0) _add(Segment(other.start + other.length - len2, len2))

        true
      case None =>
        false
    }
  }

  private def containedInSegments(seg: Segment): Option[Segment] = {
    segmentsContain.get(seg)
  }

  def totalLength: Int = _totalLength

  override def iterator: Iterator[Segment] = segments.iterator

  def firstSegment(): Segment = segments.firstKey
  def lastSegment(): Segment = segments.last
  def segmentCount: Int = segments.size
}

// TODO: make thread-safe, or rewrite it
class ChunkSegsWithKey[T] extends ChunkSegs {
  private var currentKey: T = _

  private val invMapOrder: Ordering[Segment] = { (s1, s2) =>
    if (s2.contains(s1)) 0
    else {
      val res = s1.start - s2.start
      if (res != 0) res
      else res + s1.length - s2.length
    }
  }

  private val invMap: mutable.TreeMap[Segment, T] = mutable.TreeMap.empty(invMapOrder)
  private val segmentsPerKey: mutable.HashMap[T, Int] = mutable.HashMap.empty

  override protected def _add(seg: Segment): Unit = {
    super._add(seg)
    invMap.put(seg, currentKey)
    segmentsPerKey(currentKey) = segmentsPerKey.getOrElse(currentKey, 0) + 1
  }

  override protected def _remove(seg: Segment): Unit = {
    super._remove(seg)
    invMap.remove(seg) match
      case Some(key) =>
        val newCount = segmentsPerKey.getOrElse(key, 0) - 1
        if newCount < 1 then segmentsPerKey -= key
        else segmentsPerKey(key) = newCount
      case None =>
  }

  def add(chunk: T, segment: Segment): Unit = {
    currentKey = chunk
    add(segment)
  }

  def remove(chunk: T, segment: Segment): Unit = {
    require(invMap.get(segment).contains(chunk))
    currentKey = chunk
    remove(segment)
  }

  def lastKeyAndSegment(): (T, Segment) = {
    val seg = lastSegment()
    (invMap(seg), seg)
  }

  def keyCount: Int = segmentsPerKey.size
}
