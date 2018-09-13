package com.martomate.hexacraft.world.coord.fp

import com.martomate.hexacraft.util.{CylinderSize, MathUtils}
import org.joml.Vector3d

class SkewCylCoords(_x: Double, _y: Double, _z: Double, val cylSize: CylinderSize, fixZ: Boolean = true)
  extends AbstractCoords[SkewCylCoords](_x, _y, if (fixZ) MathUtils.fitZ(_z, cylSize.circumference) else _z) {

  def toNormalCoords(reference: CylCoords): NormalCoords = toCylCoords.toNormalCoords(reference)
  def toCylCoords: CylCoords = new CylCoords(x * CylinderSize.y60, y, z + x * 0.5, cylSize, fixZ)
  def toBlockCoords: BlockCoords = new BlockCoords(x / CylinderSize.y60, y / 0.5, z / CylinderSize.y60, cylSize, fixZ)

  def +(that: SkewCylCoords) = SkewCylCoords(x + that.x, y + that.y, z + that.z, cylSize)
  def -(that: SkewCylCoords) = SkewCylCoords(x - that.x, y - that.y, z - that.z, cylSize)
}

/** CylCoords with the x axis rotated 30 deg */
object SkewCylCoords {
  def apply(vec: Vector3d, cylSize: CylinderSize) = new SkewCylCoords(vec.x, vec.y, vec.z, cylSize)
  def apply(_x: Double, _y: Double, _z: Double, cylSize: CylinderSize) = new SkewCylCoords(_x, _y, _z, cylSize)
}
