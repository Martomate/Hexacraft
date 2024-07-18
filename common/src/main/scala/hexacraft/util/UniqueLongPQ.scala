package hexacraft.util

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class UniqueLongPQ(func: Long => Double, ord: Ordering[Double]) { // PQ with fast lookup and resorting
  private type DS = (Double, Long)

  private val pq: mutable.PriorityQueue[DS] = mutable.PriorityQueue.empty(using ord.on(_._1))
  private val set: LongSet = new LongSet

  def enqueue(elem: Long): Unit = {
    if set.add(elem) then {
      pq.enqueue((func(elem), elem))
    }
  }

  def dequeue(): Long = {
    val elem = pq.dequeue()._2
    set.remove(elem)
    elem
  }

  def size: Int = pq.size

  def isEmpty: Boolean = pq.isEmpty

  def clear(): Unit = {
    pq.clear()
    set.clear()
  }

  def reprioritizeAndFilter(filterFunc: DS => Boolean): Unit = {
    set.clear()
    val buffer = new ArrayBuffer[DS](pq.size)
    for t <- pq do {
      val elem = (func(t._2), t._2)
      if filterFunc(elem) then {
        buffer += elem
        set.add(t._2)
      }
    }
    pq.clear()
    val seq = buffer.toSeq
    pq.enqueue(seq*)
  }
}
