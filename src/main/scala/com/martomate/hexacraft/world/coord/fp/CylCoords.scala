package com.martomate.hexacraft.world.coord.fp

import com.martomate.hexacraft.util.{CylinderSize, MathUtils}
import org.joml.Vector3d

class CylCoords private (_x: Double, _y: Double, _z: Double)(implicit
    val cylSize: CylinderSize
) extends AbstractCoords[CylCoords](
      _x,
      _y,
      MathUtils.fitZ(_z, cylSize.circumference)
    ) {

  def toNormalCoords(ref: CylCoords): NormalCoords = {
    val mult = math.exp((this.y - ref.y) / cylSize.radius)
    val v = (this.z - ref.z) / CylinderSize.y60 * cylSize.hexAngle
    val z = math.sin(v)
    val y = math.cos(v)

    val scale = cylSize.radius // / math.sqrt(z * z + y * y)
    NormalCoords((this.x - ref.x) * mult, y * scale * mult - cylSize.radius, z * scale * mult)
  }
  def toSkewCylCoords: SkewCylCoords =
    SkewCylCoords(x / CylinderSize.y60, y, z - x * 0.5 / CylinderSize.y60)
  def toBlockCoords: BlockCoords = toSkewCylCoords.toBlockCoords

  def distanceSq(c: CylCoords): Double = {
    val dx = x - c.x
    val dy = y - c.y
    val dz1 = MathUtils.fitZ(z - c.z, cylSize.circumference)
    val dz = math.min(dz1, cylSize.circumference - dz1)
    dx * dx + dy * dy + dz * dz
  }

  def distanceXZSq(c: CylCoords): Double = {
    val dx = x - c.x
    val dz1 = MathUtils.fitZ(z - c.z, cylSize.circumference)
    val dz = math.min(dz1, cylSize.circumference - dz1)
    dx * dx + dz * dz
  }

  def angleXZ(c: CylCoords): Double = {
    val dx = c.x - x
    // dz: -circ / 2 until circ / 2
    val dz = MathUtils.fitZ(
      c.z - z + cylSize.circumference / 2,
      cylSize.circumference
    ) - cylSize.circumference / 2
    math.atan2(dz, dx)
  }

  override def offset(dx: Double, dy: Double, dz: Double): CylCoords = CylCoords(x + dx, y + dy, z + dz)
  def offset(v: Vector3d): CylCoords = offset(v.x, v.y, v.z)
  def offset(v: CylCoords.Offset): CylCoords = offset(v.x, v.y, v.z)
}

/** NormalCoords with z axis wrapped around a cylinder. The y axis is perpendicular to the x and z
  * axes and also exponential
  */
object CylCoords {
  def apply(vec: Vector3d)(implicit cylSize: CylinderSize): CylCoords = new CylCoords(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double)(implicit cylSize: CylinderSize): CylCoords = new CylCoords(_x, _y, _z)

  case class Offset(_x: Double, _y: Double, _z: Double) extends AbstractCoords[CylCoords.Offset](_x, _y, _z) {
    def toSkewCylCoordsOffset: SkewCylCoords.Offset =
      SkewCylCoords.Offset(x / CylinderSize.y60, y, z - x * 0.5 / CylinderSize.y60)
    def toBlockCoordsOffset: BlockCoords.Offset = toSkewCylCoordsOffset.toBlockCoordsOffset

    override def offset(dx: Double, dy: Double, dz: Double): CylCoords.Offset =
      CylCoords.Offset(x + dx, y + dy, z + dz)

    def offset(v: Vector3d): CylCoords.Offset = offset(v.x, v.y, v.z)
  }
  object Offset:
    def apply(vec: Vector3d): Offset = Offset(vec.x, vec.y, vec.z)
}
