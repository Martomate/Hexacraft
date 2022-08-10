package com.martomate.hexacraft.util

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class UniquePQ[S](func: S => Double, ord: Ordering[Double]) { // PQ with fast lookup and resorting
  private type DS = (Double, S)

  private val pq: mutable.PriorityQueue[DS] = mutable.PriorityQueue.empty(ord.on(_._1))
  private val set: mutable.Set[S] = mutable.HashSet.empty

  def enqueue(elem: S): Unit = {
    if (set.add(elem)) {
      pq.enqueue((func(elem), elem))
    }
  }

  def dequeue(): S = {
    val elem = pq.dequeue()._2
    set -= elem
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
    for (t <- pq) {
      val elem = (func(t._2), t._2)
      if (filterFunc(elem))
        buffer += elem
    }
    pq.clear()
    val seq = buffer.toSeq
    pq.enqueue(seq: _*)
    buffer.foreach(set += _._2)
  }
}
