package com.martomate.hexacraft.world.coord.fp

import com.martomate.hexacraft.util.{CylinderSize, MathUtils}
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld

class BlockCoords private (_x: Double, _y: Double, _z: Double, fixZ: Boolean)(implicit
    val cylSize: CylinderSize
) extends AbstractCoords[BlockCoords](
      _x,
      _y,
      if (fixZ) MathUtils.fitZ(_z, cylSize.totalSize) else _z
    ) {

  def toNormalCoords(reference: CylCoords): NormalCoords = toSkewCylCoords.toNormalCoords(reference)
  def toCylCoords: CylCoords = toSkewCylCoords.toCylCoords
  def toSkewCylCoords: SkewCylCoords =
    SkewCylCoords(x * CylinderSize.y60, y * 0.5, z * CylinderSize.y60, fixZ)

  override def offset(dx: Double, dy: Double, dz: Double): BlockCoords =
    BlockCoords(x + dx, y + dy, z + dz, fixZ)
  def offset(c: BlockCoords): BlockCoords = this + c
}

/** BlockCoords are like SkewCylCoords but with a different axis scale */
object BlockCoords {
  def apply(_x: Double, _y: Double, _z: Double)(implicit cylSize: CylinderSize): BlockCoords =
    apply(_x, _y, _z, fixZ = true)
  def apply(_x: Double, _y: Double, _z: Double, fixZ: Boolean)(implicit cylSize: CylinderSize) =
    new BlockCoords(_x, _y, _z, fixZ)
  def apply(c: BlockRelWorld)(implicit cylSize: CylinderSize): BlockCoords = apply(c, fixZ = true)
  def apply(c: BlockRelWorld, fixZ: Boolean)(implicit cylSize: CylinderSize) =
    new BlockCoords(c.x, c.y, c.z, fixZ)
}
