package com.martomate.hexacraft.world.coord.fp

import com.martomate.hexacraft.util.{CylinderSize, MathUtils}
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import org.joml.Vector3d

class BlockCoords(_x: Double, _y: Double, _z: Double, fixZ: Boolean = true)(implicit val cylSize: CylinderSize)
  extends AbstractCoords[BlockCoords](_x, _y, if (fixZ) MathUtils.fitZ(_z, cylSize.totalSize) else _z) {

  def toNormalCoords(reference: CylCoords): NormalCoords = toSkewCylCoords.toNormalCoords(reference)
  def toCylCoords: CylCoords = toSkewCylCoords.toCylCoords
  def toSkewCylCoords: SkewCylCoords = new SkewCylCoords(x * CylinderSize.y60, y * 0.5, z * CylinderSize.y60, fixZ)

  def +(that: BlockCoords) = BlockCoords(x + that.x, y + that.y, z + that.z)
  def -(that: BlockCoords) = BlockCoords(x - that.x, y - that.y, z - that.z)
}

/** SkewCylCoords with different axis scale */
object BlockCoords {
  def apply(vec: Vector3d)(implicit cylSize: CylinderSize) = new BlockCoords(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double)(implicit cylSize: CylinderSize) = new BlockCoords(_x, _y, _z)
  def apply(c: BlockRelWorld)(implicit cylSize: CylinderSize) = new BlockCoords(c.x, c.y, c.z)
}
