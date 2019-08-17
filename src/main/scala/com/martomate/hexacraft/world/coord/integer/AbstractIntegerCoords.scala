package com.martomate.hexacraft.world.coord.integer

abstract class AbstractIntegerCoords[T](val value: T) {
  override def equals(o: Any): Boolean = o match {
    case c: AbstractIntegerCoords[T] => c.value == value
    case _ => false
  }
}
