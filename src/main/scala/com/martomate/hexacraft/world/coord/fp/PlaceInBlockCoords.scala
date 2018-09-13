package com.martomate.hexacraft.world.coord.fp

import com.martomate.hexacraft.util.CylinderSize
import org.joml.Vector3d

class PlaceInBlockCoords(_x: Double, _y: Double, _z: Double, cylSize: CylinderSize)
  extends AbstractCoords[PlaceInBlockCoords](_x, _y, _z) {

  def toBlockCoords: BlockCoords = BlockCoords((x - 0.5 * z) * 4 / 3, y + 0.5, (z - 0.5 * x) * 4 / 3, cylSize)

  def +(that: PlaceInBlockCoords) = PlaceInBlockCoords(x + that.x, y + that.y, z + that.z, cylSize)
  def -(that: PlaceInBlockCoords) = PlaceInBlockCoords(x - that.x, y - that.y, z - that.z, cylSize)
}

/** BlockCoords with x and z replaced with how far along to x and z axes the vector reaches. y = 0 in the middle of the block. */
object PlaceInBlockCoords {
  def apply(vec: Vector3d, cylSize: CylinderSize) = new PlaceInBlockCoords(vec.x, vec.y, vec.z, cylSize)
  def apply(_x: Double, _y: Double, _z: Double, cylSize: CylinderSize) = new PlaceInBlockCoords(_x, _y, _z, cylSize)
}
