package hexacraft.world.coord.integer

import hexacraft.world.CylinderSize
import org.joml.Vector2d

object ColumnRelWorld {
  def apply(X: Long, Z: Int)(implicit cylSize: CylinderSize): ColumnRelWorld = ColumnRelWorld(
    (X & 0xfffff) << 20 | (Z & cylSize.ringSizeMask)
  )
}

case class ColumnRelWorld(value: Long) extends AnyVal { // XXXXXZZZZZ
  def X: Int = (value >> 8).toInt >> 12
  def Z: Int = (value << 12).toInt >> 12

  def offset(x: Int, z: Int)(implicit cylSize: CylinderSize): ColumnRelWorld =
    ColumnRelWorld(X + x, Z + z)

  def distSq(origin: Vector2d)(implicit cylSize: CylinderSize): Double = {
    val dx = this.X - origin.x + 0.5
    val dz1 = math.abs(this.Z - origin.y + 0.5 + dx * 0.5f)
    val dz2 = math.abs(dz1 - cylSize.ringSize)
    val dz = math.min(dz1, dz2)
    dx * dx + dz * dz
  }
}
