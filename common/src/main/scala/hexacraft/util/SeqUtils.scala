package hexacraft.util

import java.util.Random
import scala.collection.mutable

object SeqUtils {
  def whileSome[T](maxCount: Int, maker: => Option[T])(taker: T => Any): Unit = {
    Loop.rangeUntil(0, maxCount) { _ =>
      maker match {
        case Some(t) => taker(t)
        case None    => return
      }
    }
  }

  def shuffleArray[T](arr: Array[T], random: Random): Unit = {
    val len = arr.length
    Loop.rangeUntil(0, len) { i =>
      val idx = random.nextInt(len - i) + i
      val temp = arr(i)
      arr(i) = arr(idx)
      arr(idx) = temp
    }
  }

  /** @return a new sequence taking elements from the input lists in a round-robin order */
  def roundRobin[T](lists: Seq[Seq[T]]): Seq[T] = {
    val result: mutable.ArrayBuffer[T] = mutable.ArrayBuffer.empty

    val its = lists.map(_.iterator)
    var left = true
    while left do {
      left = false
      Loop.iterate(its.iterator) { it =>
        if it.hasNext then {
          left = true
          result += it.next
        }
      }
    }

    result.toSeq
  }
}
