package hexacraft.util

import java.util.Random

object SeqUtils:
  def whileSome[T](maxCount: Int, maker: => Option[T])(taker: T => Any): Unit =
    var i = 0
    while i < maxCount
    do
      maker match
        case Some(t) =>
          taker(t)
        case None =>
          i = maxCount
      i += 1

  def shuffleArray[T](arr: Array[T], random: Random): Unit =
    val len = arr.length
    for i <- 0 until len do
      val idx = random.nextInt(len - i) + i
      val temp = arr(i)
      arr(i) = arr(idx)
      arr(idx) = temp
