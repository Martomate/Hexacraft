package hexacraft.util

import scala.collection.mutable

class LongSet {
  private val sets: mutable.LongMap[mutable.BitSet] = mutable.LongMap.empty

  def add(elem: Long): Boolean = {
    val chunk = elem >>> 12
    val block = (elem & ((1 << 12) - 1)).toInt
    val set = sets.getOrElseUpdate(chunk, new mutable.BitSet(16 * 16 * 16))

    val isNew = !set.contains(block)
    if isNew then {
      set.addOne(block)
    }
    isNew
  }

  def remove(elem: Long): Unit = {
    val chunk = elem >>> 12
    val block = (elem & ((1 << 12) - 1)).toInt
    val set = sets.getOrElseUpdate(chunk, new mutable.BitSet(16 * 16 * 16))

    set.subtractOne(block)
  }

  def clear(): Unit = {
    sets.clear()
  }
}

class UniqueLongQueue {
  private val q: mutable.Queue[Long] = mutable.Queue.empty
  private val set: LongSet = new LongSet

  def enqueue(elem: Long): Unit = {
    if set.add(elem) then {
      q.enqueue(elem)
    }
  }

  def enqueueMany(elems: Iterable[Long]): Unit = {
    q.ensureSize(q.size + elems.size)

    Loop.iterate(elems.iterator) { elem =>
      if set.add(elem) then {
        q.enqueue(elem)
      }
    }
  }

  def dequeue(): Long = {
    val elem = q.dequeue()
    set.remove(elem)
    elem
  }

  inline def drainInto(inline f: Long => Unit): Unit = {
    set.clear()
    while q.nonEmpty do {
      f(q.dequeue())
    }
  }

  def size: Int = q.length

  def isEmpty: Boolean = q.isEmpty

  def clear(): Unit = {
    q.clear()
    set.clear()
  }
}
