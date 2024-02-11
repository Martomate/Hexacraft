package hexacraft.util

import scala.collection.mutable

case class Segment(start: Int, length: Int) {
  require(start >= 0, "'start' has to be non-negative")
  require(length > 0, "'length' has to be positive")

  def contains(inner: Segment): Boolean = {
    inner.start >= start && inner.start + inner.length <= start + length
  }

  def overlaps(other: Segment): Boolean = {
    other.start < start + length && other.start + other.length > start
  }
}

class SegmentSet extends mutable.Iterable[Segment] {

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

  /** segment `[a, b]` has to either exist as `[a, b]`
    * or be part of a bigger existing segment `[c, d]` where `c <= a` and `d >= b`
    */
  def remove(seg: Segment): Boolean = {
    containedInSegments(seg) match {
      case Some(other) =>
        val len1 = seg.start - other.start
        val len2 = other.length - len1 - seg.length

        _remove(other)
        if len1 > 0 then {
          _add(Segment(other.start, len1))
        }
        if len2 > 0 then {
          _add(Segment(other.start + other.length - len2, len2))
        }

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
class KeyedSegmentSet[T] extends SegmentSet {
  private var currentKey: T = _

  private val invMapOrder: Ordering[Segment] = { (s1, s2) =>
    if s2.contains(s1) then {
      0
    } else {
      val res = s1.start - s2.start
      if res != 0 then {
        res
      } else {
        res + s1.length - s2.length
      }
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
    invMap.remove(seg) match {
      case Some(key) =>
        val newCount = segmentsPerKey.getOrElse(key, 0) - 1
        if newCount < 1 then {
          segmentsPerKey -= key
        } else {
          segmentsPerKey(key) = newCount
        }
      case None =>
    }
  }

  def add(key: T, segment: Segment): Unit = {
    currentKey = key
    add(segment)
  }

  def remove(key: T, segment: Segment): Unit = {
    require(invMap.get(segment).contains(key))
    currentKey = key
    remove(segment)
  }

  def lastKeyAndSegment: (T, Segment) = {
    val seg = lastSegment()
    (invMap(seg), seg)
  }

  def keyCount: Int = segmentsPerKey.size
}

/** A dense stack of keyed segments, which means that:
  *   1. there are no gaps between the segments
  *   1. segments can only be added/removed at the end
  *   1. adjacent segments can only be merged if they have the same key
  *
  * It can also be thought of as a contiguous piece of memory where the bytes are colored.
  */
class DenseKeyedSegmentStack[K] {
  private val contentMap: mutable.Map[K, SegmentSet] = mutable.Map.empty
  private val allSegments: KeyedSegmentSet[K] = new KeyedSegmentSet

  def length: Int = lastSegment.map(s => s._2.start + s._2.length).getOrElse(0)

  def fragmentation: Float = allSegments.segmentCount.toFloat / allSegments.keyCount

  def hasMapping(key: K): Boolean = {
    contentMap.get(key).exists(_.totalLength != 0)
  }

  def totalLengthForChunk(key: K): Int = {
    contentMap.get(key).map(_.totalLength).getOrElse(0)
  }

  /** Creates a new segment on top of the stack and labels it with the given key */
  def push(key: K, numBytes: Int): Segment = {
    val segment = Segment(length, numBytes)
    this.add(key, segment)
    segment
  }

  /** Sets the key for the segment
    *
    * @throws IllegalArgumentException if the segment does not belong to `from`
    */
  def relabel(from: K, to: K, segment: Segment): Unit = {
    this.remove(from, segment)
    this.add(to, segment)
  }

  /** Removes the given segment from the top of the stack
    *
    * @throws IllegalArgumentException if the segment is not the top of the stack
    * @throws IllegalArgumentException if the segment does not belong to the given key
    */
  def pop(key: K, segment: Segment): Unit = {
    require(segment.start + segment.length == length)
    this.remove(key, segment)
  }

  private def add(key: K, segment: Segment): Unit = {
    contentMap.getOrElseUpdate(key, new SegmentSet).add(segment)
    allSegments.add(key, segment)
  }

  private def remove(key: K, segment: Segment): Unit = {
    allSegments.remove(key, segment)
    contentMap.get(key).exists(_.remove(segment))
  }

  def segments(key: K): Iterable[Segment] = {
    contentMap.getOrElse(key, Iterable.empty)
  }

  def lastSegment: Option[(K, Segment)] = {
    if allSegments.totalLength > 0 then {
      Some(allSegments.lastKeyAndSegment)
    } else {
      None
    }
  }
}
