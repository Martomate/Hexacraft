package com.martomate.hexacraft.world.coord.fp

import com.martomate.hexacraft.util.{CylinderSize, MathUtils}
import org.joml.Vector3d

class CylCoords(_x: Double, _y: Double, _z: Double, fixZ: Boolean = true)(implicit val cylSize: CylinderSize)
  extends AbstractCoords[CylCoords](_x, _y, if (fixZ) MathUtils.fitZ(_z, cylSize.circumference) else _z) {

  def toNormalCoords(ref: CylCoords): NormalCoords = {
    val mult = math.exp((this.y - ref.y) / cylSize.radius)
    val v = (this.z - ref.z) / CylinderSize.y60 * cylSize.hexAngle
    val z = math.sin(v)
    val y = math.cos(v)

    val scale = cylSize.radius // / math.sqrt(z * z + y * y)
    NormalCoords((this.x - ref.x) * mult, y * scale * mult - cylSize.radius, z * scale * mult)
  }
  def toSkewCylCoords: SkewCylCoords = new SkewCylCoords(x / CylinderSize.y60, y, z - x * 0.5 / CylinderSize.y60, fixZ)
  def toBlockCoords: BlockCoords = toSkewCylCoords.toBlockCoords

  def +(that: CylCoords) = CylCoords(x + that.x, y + that.y, z + that.z)
  def -(that: CylCoords) = CylCoords(x - that.x, y - that.y, z - that.z)

  def distanceSq(c: CylCoords): Double = {
    val dx = x - c.x
    val dy = y - c.y
    val dz1 = MathUtils.fitZ(z - c.z, cylSize.circumference)
    val dz = math.min(dz1, cylSize.circumference - dz1)
    dx * dx + dy * dy + dz * dz
  }
}

/** NormalCoords with z axis wrapped around a cylinder. The y axis is perpendicular to the x and z axes and also exponential */
object CylCoords {
  def apply(vec: Vector3d)(implicit cylSize: CylinderSize) = new CylCoords(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double)(implicit cylSize: CylinderSize) = new CylCoords(_x, _y, _z)
}
