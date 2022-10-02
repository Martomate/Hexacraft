package com.martomate.hexacraft.world.coord.fp

import org.joml.Vector3d

class NormalCoords private (_x: Double, _y: Double, _z: Double) extends AbstractCoords[NormalCoords](_x, _y, _z) {
  def toCylCoords(reference: NormalCoords): CylCoords = ???
  def toSkewCylCoords(reference: NormalCoords): SkewCylCoords = toCylCoords(
    reference
  ).toSkewCylCoords
  def toBlockCoords(reference: NormalCoords): BlockCoords = toCylCoords(reference).toBlockCoords

  override def offset(dx: Double, dy: Double, dz: Double): NormalCoords =
    NormalCoords(x + dx, y + dy, z + dz)
}

object NormalCoords {
  def apply(vec: Vector3d) = new NormalCoords(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double) = new NormalCoords(_x, _y, _z)
}
