package com.martomate.hexacraft.world.coord.fp

import com.martomate.hexacraft.util.CylinderSize
import org.joml.Vector3d

class NormalCoords private (_x: Double, _y: Double, _z: Double) extends AbstractCoords[NormalCoords](_x, _y, _z) {
  def toCylCoords(reference: NormalCoords): CylCoords = ???
  def toSkewCylCoords(reference: NormalCoords)(using CylinderSize): SkewCylCoords = toCylCoords(
    reference
  ).toSkewCylCoords
  def toBlockCoords(reference: NormalCoords)(using CylinderSize): BlockCoords = toCylCoords(reference).toBlockCoords

  override def offset(dx: Double, dy: Double, dz: Double)(using CylinderSize): NormalCoords =
    NormalCoords(x + dx, y + dy, z + dz)
}

object NormalCoords {
  def apply(vec: Vector3d) = new NormalCoords(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double) = new NormalCoords(_x, _y, _z)
}
