package com.martomate.hexacraft.util

import scala.collection.mutable

class SmartArray[@specialized(Byte) T](size: Int, default: T, builder: Int => Array[T]) extends mutable.IndexedSeq[T] {
  private var arr: Array[T] = _

  def apply(idx: Int): T = if (arr != null) arr(idx) else default

  def update(idx: Int, value: T): Unit = {
    if (arr == null && value != default) arr = builder(size)

    if (arr != null) arr(idx) = value
  }

  override def length: Int = size
}

object SmartArray {
  def apply[T](size: Int, default: T)(builder: Int => Array[T]): SmartArray[T] =
    new SmartArray[T](size, default, builder)

  def withByteArray(size: Int, default: Byte): SmartArray[Byte] =
    new SmartArray[Byte](size, default, size => new Array(size))
}
