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

  private val segments: mutable.TreeSet[Segment] = mutable.TreeSet.empty(using { (s1, s2) =>
    if (s1.overlaps(s2)) 0
    else s1.start - s2.start
  })
  private val segmentsContain: mutable.TreeMap[Segment, Segment] = mutable.TreeMap.empty(using { (s1, s2) =>
    if (s2.contains(s1)) 0
    else s1.start - s2.start
  })
  private var _totalLength = 0

  private def _add(seg: Segment): Unit = {
    val wasAdded = segments.add(seg)
    require(wasAdded, s"$seg cannot be added")

    segmentsContain(seg) = seg
    _totalLength += seg.length
  }

  private def _remove(seg: Segment): Unit = {
    val didExist = segmentsContain.remove(seg).isDefined
    require(didExist, s"$seg cannot be removed")

    segments.remove(seg)
    _totalLength -= seg.length
  }

  def add(seg: Segment): Unit = {
    require(!segments.contains(seg))
    val before = segments.maxBefore(seg)
    val after = segments.minAfter(seg)

    var newSegment = seg
    before match {
      case Some(b) =>
        if b.start + b.length == newSegment.start then {
          newSegment = Segment(b.start, b.length + newSegment.length)
          _remove(b)
        }
      case None =>
    }
    after match {
      case Some(a) =>
        if a.start == newSegment.start + newSegment.length then {
          newSegment = Segment(newSegment.start, newSegment.length + a.length)
          _remove(a)
        }
      case None =>
    }
    _add(newSegment)
  }

  /** segment `[a, b]` has to either exist as `[a, b]`
    * or be part of a bigger existing segment `[c, d]` where `c <= a` and `d >= b`
    */
  def remove(seg: Segment): Boolean = {
    segmentsContain.get(seg) match {
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

  def cut(n: Int): (SegmentSet, SegmentSet) = {
    val before = new SegmentSet
    val after = new SegmentSet

    var start = 0
    for s <- this do {
      if start + s.length <= n then {
        before.add(s)
      } else if start >= n then {
        after.add(s)
      } else {
        val l = n - start
        before.add(Segment(s.start, l))
        after.add(Segment(s.start + l, s.length - l))
      }
      start += s.length
    }

    (before, after)
  }

  def totalLength: Int = _totalLength

  override def iterator: Iterator[Segment] = segments.iterator

  def firstSegment(): Segment = segments.firstKey
  def lastSegment(): Segment = segments.last

  def allocate(length: Int): SegmentSet = {
    val result = SegmentSet()

    if length == 0 then {
      return result
    } else if segments.isEmpty then {
      val s = Segment(0, length)
      add(s)
      result.add(s)
      return result
    }

    var left = length
    while left > 0 do {
      val it = segments.iterator
      val first = it.next()
      it.nextOption() match {
        case Some(second) =>
          val start = first.start + first.length
          val space = second.start - start
          if left < space then {
            val s = Segment(start, left)
            result.add(s)
            add(s)
            left = 0
          } else {
            val s = Segment(start, space)
            result.add(s)
            add(s)
            left -= space
          }
        case None =>
          val start = first.start + first.length
          val s = Segment(start, left)
          result.add(s)
          add(s)
          left = 0
      }
    }
    result
  }

  override def clone(): SegmentSet = {
    val res = new SegmentSet
    for s <- this do {
      res._add(s)
    }
    res
  }
}

class KeyedSegment[K] {
  private val keyedSegments: mutable.HashMap[K, SegmentSet] = mutable.HashMap.empty
  private val allSegments: SegmentSet = SegmentSet()

  def allocate(key: K, length: Int): SegmentSet = {
    val segmentsForKey = keyedSegments.getOrElseUpdate(key, SegmentSet())
    val newSegments = allSegments.allocate(length)
    for s <- newSegments do {
      segmentsForKey.add(s)
    }
    newSegments
  }

  def clear(key: K): Unit = {
    for {
      segmentSet <- keyedSegments.remove(key)
      segment <- segmentSet
    } do {
      allSegments.remove(segment)
    }
  }

  def usedSegments: SegmentSet = {
    allSegments.clone()
  }

  def isEmpty: Boolean = {
    allSegments.totalLength == 0
  }

  def numKeyedSegments: Int = {
    keyedSegments.values.map(_.size).sum
  }

  def numKeys: Int = {
    keyedSegments.keys.size
  }
}
