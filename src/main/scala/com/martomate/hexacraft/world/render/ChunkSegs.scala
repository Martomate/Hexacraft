package com.martomate.hexacraft.world.render

import scala.collection.mutable

class ChunkSegs extends mutable.Iterable[Segment] {

  private val segments: mutable.TreeSet[Segment] = mutable.TreeSet.empty(_.start - _.start)
  private var _totalLength = 0

  protected def _add(seg: Segment): Unit = {
    require(segments.add(seg))
    _totalLength += seg.length
  }
  protected def _remove(seg: Segment): Unit = {
    require(segments.remove(seg))
    _totalLength -= seg.length
  }

  def add(seg: Segment): Unit = {
    require(!segments.exists(_.overlaps(seg)))
    _add(seg)
  }

  /** segment [a, b] has to either exist as [a, b] or be part of a bigger existing segment [c, d], c <= a, d >= b */
  def remove(seg: Segment): Boolean = {
    segments.find(_.contains(seg)) match {
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

  def totalLength: Int = _totalLength

  override def iterator: Iterator[Segment] = segments.iterator

  def firstSegment(): Segment = segments.firstKey
  def lastSegment(): Segment = segments.last
}

case class Segment(start: Int, length: Int) {
  def contains(inner: Segment): Boolean = inner.start >= start && inner.start + inner.length <= start + length

  def overlaps(other: Segment): Boolean = other.start < start + length && other.start + other.length > start
}
