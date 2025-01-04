package hexacraft.util

import scala.collection.mutable.ArrayBuffer

/** This class can be used to speed up for-loops, and works similarly to the functions in `Loop` */
class InlinedIterable[A](items: Iterable[A]) extends AnyVal {
  inline def foreach(inline f: A => Unit): Unit = {
    val it = items.iterator
    while it.hasNext do {
      f(it.next)
    }
  }

  inline def withFilter[F <: A => Boolean](inline f: F): InlinedIterable.WithFilter[A, F] = {
    InlinedIterable.WithFilter(items, f)
  }

  inline def map[B](inline f: A => B): Iterable[B] = {
    val res = new ArrayBuffer[B](items.size)
    val it = items.iterator
    while it.hasNext do {
      val item = it.next
      res += f(item)
    }
    res
  }
}

object InlinedIterable {
  class WithFilter[A, F <: A => Boolean](fields: (Iterable[A], F)) extends AnyVal {
    inline def foreach(inline f: A => Unit): Unit = {
      val items = fields._1
      val filter = fields._2

      val it = items.iterator
      while it.hasNext do {
        val item = it.next
        if filter(item) then {
          f(item)
        }
      }
    }

    inline def map[B](inline f: A => B): Iterable[B] = {
      val items = fields._1
      val filter = fields._2

      val res = new ArrayBuffer[B](items.size)
      val it = items.iterator
      while it.hasNext do {
        val item = it.next
        if filter(item) then {
          res += f(item)
        }
      }
      res
    }
  }
}
