package com.martomate.hexacraft.world.coord.fp

import com.martomate.hexacraft.util.{CylinderSize, MathUtils}

class SkewCylCoords private (_x: Double, _y: Double, _z: Double, fixZ: Boolean)(implicit
    val cylSize: CylinderSize
) extends AbstractCoords[SkewCylCoords](
      _x,
      _y,
      if (fixZ) MathUtils.fitZ(_z, cylSize.circumference) else _z
    ) {

  def toNormalCoords(reference: CylCoords): NormalCoords = toCylCoords.toNormalCoords(reference)
  def toCylCoords: CylCoords = CylCoords(x * CylinderSize.y60, y, z + x * 0.5, fixZ)
  def toBlockCoords: BlockCoords =
    BlockCoords(x / CylinderSize.y60, y / 0.5, z / CylinderSize.y60, fixZ)

  override def offset(dx: Double, dy: Double, dz: Double): SkewCylCoords =
    SkewCylCoords(x + dx, y + dy, z + dz, fixZ)
}

/** CylCoords with the x axis rotated 30 deg */
object SkewCylCoords {
  def apply(_x: Double, _y: Double, _z: Double, fixZ: Boolean)(implicit cylSize: CylinderSize) =
    new SkewCylCoords(_x, _y, _z, fixZ)
}
