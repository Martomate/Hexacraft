package hexacraft.world.coord.integer

import hexacraft.math.Int20
import hexacraft.world.CylinderSize

import org.joml.Vector2d

object ColumnRelWorld {
  def apply(X: Long, Z: Int)(using cylSize: CylinderSize): ColumnRelWorld =
    ColumnRelWorld((X & 0xfffff) << 20 | (Z & cylSize.ringSizeMask))
}

case class ColumnRelWorld(value: Long) extends AnyVal { // XXXXXZZZZZ
  def X: Int20 = Int20.truncate(value >> 20)
  def Z: Int20 = Int20.truncate(value)

  def offset(x: Int, z: Int)(using CylinderSize): ColumnRelWorld = ColumnRelWorld(X.toInt + x, Z.toInt + z)

  def distSq(origin: Vector2d)(using cylSize: CylinderSize): Double =
    val dx = this.X.toInt - origin.x + 0.5
    val dz1 = math.abs(this.Z.toInt - origin.y + 0.5 + dx * 0.5f)
    val dz2 = math.abs(dz1 - cylSize.ringSize)
    val dz = math.min(dz1, dz2)
    dx * dx + dz * dz
}
