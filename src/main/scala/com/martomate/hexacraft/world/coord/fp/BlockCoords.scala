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
  def offset(c: BlockCoords.Offset): BlockCoords = offset(c.x, c.y, c.z)
}

/** BlockCoords are like SkewCylCoords but with a different axis scale */
object BlockCoords {
  def apply(_x: Double, _y: Double, _z: Double)(implicit cylSize: CylinderSize): BlockCoords =
    apply(_x, _y, _z, fixZ = true)
  def apply(c: BlockRelWorld)(implicit cylSize: CylinderSize): BlockCoords = new BlockCoords(c.x, c.y, c.z, fixZ = true)

  def apply(_x: Double, _y: Double, _z: Double, fixZ: Boolean)(implicit cylSize: CylinderSize) =
    new BlockCoords(_x, _y, _z, fixZ)

  case class Offset(_x: Double, _y: Double, _z: Double) extends AbstractCoords[BlockCoords.Offset](_x, _y, _z) {
    def toCylCoordsOffset: CylCoords.Offset = toSkewCylCoordsOffset.toCylCoordsOffset
    def toSkewCylCoordsOffset: SkewCylCoords.Offset =
      SkewCylCoords.Offset(x * CylinderSize.y60, y * 0.5, z * CylinderSize.y60)

    override def offset(dx: Double, dy: Double, dz: Double): BlockCoords.Offset =
      BlockCoords.Offset(x + dx, y + dy, z + dz)

    def offset(c: BlockCoords.Offset): BlockCoords.Offset = this + c
  }
}
