package hexacraft.util

import scala.collection.mutable

class UniqueQueue[S]:
  private val q: mutable.Queue[S] = mutable.Queue.empty
  private val set: mutable.Set[S] = mutable.HashSet.empty

  def enqueue(elem: S): Unit =
    if set.add(elem)
    then q.enqueue(elem)

  def dequeue(): S =
    val elem = q.dequeue()
    set -= elem
    elem

  def size: Int = q.size

  def isEmpty: Boolean = q.isEmpty

  def clear(): Unit =
    q.clear()
    set.clear()
