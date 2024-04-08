package hexacraft.util

import scala.collection.mutable

class SmartArray[@specialized(Byte) T](size: Int, default: T, builder: Int => Array[T]) extends mutable.IndexedSeq[T] {
  private var arr: Array[T] = null.asInstanceOf[Array[T]]

  def apply(idx: Int): T = {
    if arr != null then {
      arr(idx)
    } else {
      default
    }
  }

  def update(idx: Int, value: T): Unit = {
    if arr == null && value != default then {
      arr = builder(size)
      for i <- 0 until size do {
        arr(i) = default
      }
    }

    if arr != null then {
      arr(idx) = value
    }
  }

  override def length: Int = size
}

object SmartArray {
  def apply[T](size: Int, default: T)(builder: Int => Array[T]): SmartArray[T] = {
    new SmartArray[T](size, default, builder)
  }

  def withByteArray(size: Int, default: Byte): SmartArray[Byte] = {
    new SmartArray[Byte](size, default, size => new Array(size))
  }
}
