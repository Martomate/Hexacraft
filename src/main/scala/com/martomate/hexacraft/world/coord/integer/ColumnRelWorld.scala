package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize
import org.joml.Vector2d

object ColumnRelWorld {
  def apply(X: Long, Z: Int, cylSize: CylinderSize): ColumnRelWorld = ColumnRelWorld((X & 0xfffff) << 20 | (Z & cylSize.ringSizeMask), cylSize)
}

case class ColumnRelWorld(private val _value: Long, cylSize: CylinderSize) extends AbstractIntegerCoords(_value) { // XXXXXZZZZZ
  def X: Int = (value >> 8).toInt >> 12
  def Z: Int = (value << 12).toInt >> 12

  def offset(x: Int, z: Int): ColumnRelWorld = ColumnRelWorld(X + x, Z + z, cylSize)

  def distSq(origin: Vector2d): Double = {
    val dx = this.X - origin.x + 0.5
    val dz1 = math.abs(this.Z - origin.y + 0.5 + dx * 0.5f)
    val dz2 = math.abs(dz1 - cylSize.ringSize)
    val dz = math.min(dz1, dz2)
    dx * dx + dz * dz
  }
}