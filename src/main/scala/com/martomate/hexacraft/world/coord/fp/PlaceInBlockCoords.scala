package com.martomate.hexacraft.world.coord.fp

import com.martomate.hexacraft.util.CylinderSize
import org.joml.Vector3d

class PlaceInBlockCoords(_x: Double, _y: Double, _z: Double)(implicit val cylSize: CylinderSize)
  extends AbstractCoords[PlaceInBlockCoords](_x, _y, _z) {

  def toBlockCoords: BlockCoords = BlockCoords((x - 0.5 * z) * 4 / 3, y + 0.5, (z - 0.5 * x) * 4 / 3)

  def +(that: PlaceInBlockCoords): PlaceInBlockCoords = PlaceInBlockCoords(x + that.x, y + that.y, z + that.z)
  def -(that: PlaceInBlockCoords): PlaceInBlockCoords = PlaceInBlockCoords(x - that.x, y - that.y, z - that.z)
}

/** BlockCoords with x and z replaced with how far along to x and z axes the vector reaches. y = 0 in the middle of the block. */
object PlaceInBlockCoords {
  def apply(vec: Vector3d)(implicit cylSize: CylinderSize) = new PlaceInBlockCoords(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double)(implicit cylSize: CylinderSize) = new PlaceInBlockCoords(_x, _y, _z)

  def fromBlockCoords(b: BlockCoords)(implicit cylSize: CylinderSize): PlaceInBlockCoords = this(b.x + 0.5 * b.z, b.y - 0.5, b.z + 0.5 * b.x)
}
