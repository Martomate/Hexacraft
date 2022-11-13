package com.martomate.hexacraft.world.coord.fp

import com.martomate.hexacraft.util.{CylinderSize, MathUtils}
import com.martomate.hexacraft.world.coord.fp

class SkewCylCoords private (_x: Double, _y: Double, _z: Double)(implicit
    val cylSize: CylinderSize
) extends AbstractCoords[SkewCylCoords](_x, _y, _z) {

  def toNormalCoords(reference: CylCoords): NormalCoords = toCylCoords.toNormalCoords(reference)
  def toCylCoords: CylCoords = CylCoords(x * CylinderSize.y60, y, z + x * 0.5)
  def toBlockCoords: BlockCoords =
    BlockCoords(x / CylinderSize.y60, y / 0.5, z / CylinderSize.y60)

  override def offset(dx: Double, dy: Double, dz: Double): SkewCylCoords =
    SkewCylCoords(x + dx, y + dy, z + dz)

  def offset(v: fp.SkewCylCoords.Offset): SkewCylCoords = offset(v.x, v.y, v.z)
}

/** CylCoords with the x axis rotated 30 deg */
object SkewCylCoords {
  def apply(_x: Double, _y: Double, _z: Double)(implicit cylSize: CylinderSize) =
    new SkewCylCoords(_x, _y, MathUtils.fitZ(_z, cylSize.circumference))

  case class Offset(_x: Double, _y: Double, _z: Double) extends AbstractCoords[SkewCylCoords.Offset](_x, _y, _z) {
    def toCylCoordsOffset: CylCoords.Offset = CylCoords.Offset(x * CylinderSize.y60, y, z + x * 0.5)
    def toBlockCoordsOffset: BlockCoords.Offset =
      BlockCoords.Offset(x / CylinderSize.y60, y / 0.5, z / CylinderSize.y60)

    override def offset(dx: Double, dy: Double, dz: Double): SkewCylCoords.Offset =
      SkewCylCoords.Offset(x + dx, y + dy, z + dz)

    def offset(c: SkewCylCoords.Offset): SkewCylCoords.Offset = this + c
  }
}
