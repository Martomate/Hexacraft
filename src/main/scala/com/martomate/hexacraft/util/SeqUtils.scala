package com.martomate.hexacraft.util

object SeqUtils {
  def whileSome[T](maxCount: Int, maker: =>Option[T])(taker: T => Any): Unit = {
    var i = 0
    while (i < maxCount) {
      maker match {
        case Some(t) =>
          taker(t)
        case None =>
          i = maxCount
      }
      i += 1
    }
  }
}
