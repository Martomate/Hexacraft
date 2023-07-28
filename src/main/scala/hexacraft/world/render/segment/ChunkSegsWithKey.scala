package hexacraft.world.render.segment

import scala.collection.mutable

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

  override protected def _add(seg: Segment): Unit = {
    super._add(seg)
    invMap.put(seg, currentKey)
  }

  override protected def _remove(seg: Segment): Unit = {
    super._remove(seg)
    invMap.remove(seg)
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
}
