package com.martomate.hexacraft.world.coord

import com.martomate.hexacraft.world.storage.CylinderSize
import org.joml.Vector3d

sealed abstract class AbstractCoords[T <: AbstractCoords[T]](val x: Double, val y: Double, val z: Double) {
  def +(that: T): T
  def -(that: T): T
  def into(vec: Vector3d): Vector3d = vec.set(x, y, z)
  def toVector3d: Vector3d = into(new Vector3d)

  override def toString: String = f"($x%.3f, $y%.3f, $z%.3f)"
}

class NormalCoords(_x: Double, _y: Double, _z: Double) extends AbstractCoords[NormalCoords](_x, _y, _z) {
  def toCylCoords(reference: NormalCoords): CylCoords = ???
  def toSkewCylCoords(reference: NormalCoords): SkewCylCoords = toCylCoords(reference).toSkewCylCoords
  def toBlockCoords(reference: NormalCoords): BlockCoords = toCylCoords(reference).toBlockCoords

  def +(that: NormalCoords) = NormalCoords(x + that.x, y + that.y, z + that.z)
  def -(that: NormalCoords) = NormalCoords(x - that.x, y - that.y, z - that.z)
}

class CylCoords(_x: Double, _y: Double, _z: Double, val cylSize: CylinderSize, fixZ: Boolean = true)
  extends AbstractCoords[CylCoords](_x, _y, if (fixZ) CoordUtils.fitZ(_z, cylSize.circumference) else _z) {

  def toNormalCoords(ref: CylCoords): NormalCoords = {
    val mult = math.exp((this.y - ref.y) / cylSize.radius)
    val v = (this.z - ref.z) / CoordUtils.y60 * cylSize.hexAngle
    val z = math.sin(v)
    val y = math.cos(v)

    val scale = cylSize.radius // / math.sqrt(z * z + y * y)
    NormalCoords((this.x - ref.x) * mult, y * scale * mult - cylSize.radius, z * scale * mult)
  }
  def toSkewCylCoords: SkewCylCoords = new SkewCylCoords(x / CoordUtils.y60, y, z - x * 0.5 / CoordUtils.y60, cylSize, fixZ)
  def toBlockCoords: BlockCoords = toSkewCylCoords.toBlockCoords

  def +(that: CylCoords) = CylCoords(x + that.x, y + that.y, z + that.z, cylSize)
  def -(that: CylCoords) = CylCoords(x - that.x, y - that.y, z - that.z, cylSize)

  def distanceSq(c: CylCoords): Double = {
    val dx = x - c.x
    val dy = y - c.y
    val dz1 = CoordUtils.fitZ(z - c.z, cylSize.circumference)
    val dz = math.min(dz1, cylSize.circumference - dz1)
    dx * dx + dy * dy + dz * dz
  }
}

class SkewCylCoords(_x: Double, _y: Double, _z: Double, val cylSize: CylinderSize, fixZ: Boolean = true)
  extends AbstractCoords[SkewCylCoords](_x, _y, if (fixZ) CoordUtils.fitZ(_z, cylSize.circumference) else _z) {

  def toNormalCoords(reference: CylCoords): NormalCoords = toCylCoords.toNormalCoords(reference)
  def toCylCoords: CylCoords = new CylCoords(x * CoordUtils.y60, y, z + x * 0.5, cylSize, fixZ)
  def toBlockCoords: BlockCoords = new BlockCoords(x / CoordUtils.y60, y / 0.5, z / CoordUtils.y60, cylSize, fixZ)

  def +(that: SkewCylCoords) = SkewCylCoords(x + that.x, y + that.y, z + that.z, cylSize)
  def -(that: SkewCylCoords) = SkewCylCoords(x - that.x, y - that.y, z - that.z, cylSize)
}

class BlockCoords(_x: Double, _y: Double, _z: Double, val cylSize: CylinderSize, fixZ: Boolean = true)
  extends AbstractCoords[BlockCoords](_x, _y, if (fixZ) CoordUtils.fitZ(_z, cylSize.totalSize) else _z) {

  def toNormalCoords(reference: CylCoords): NormalCoords = toSkewCylCoords.toNormalCoords(reference)
  def toCylCoords: CylCoords = toSkewCylCoords.toCylCoords
  def toSkewCylCoords: SkewCylCoords = new SkewCylCoords(x * CoordUtils.y60, y * 0.5, z * CoordUtils.y60, cylSize, fixZ)
  def toPlaceInBlockCoords: PlaceInBlockCoords = PlaceInBlockCoords(x + 0.5 * z, y - 0.5, z + 0.5 * x, cylSize)

  def +(that: BlockCoords) = BlockCoords(x + that.x, y + that.y, z + that.z, cylSize)
  def -(that: BlockCoords) = BlockCoords(x - that.x, y - that.y, z - that.z, cylSize)
}

class PlaceInBlockCoords(_x: Double, _y: Double, _z: Double, cylSize: CylinderSize)
  extends AbstractCoords[PlaceInBlockCoords](_x, _y, _z) {

  def toBlockCoords: BlockCoords = BlockCoords((x - 0.5 * z) * 4 / 3, y + 0.5, (z - 0.5 * x) * 4 / 3, cylSize)

  def +(that: PlaceInBlockCoords) = PlaceInBlockCoords(x + that.x, y + that.y, z + that.z, cylSize)
  def -(that: PlaceInBlockCoords) = PlaceInBlockCoords(x - that.x, y - that.y, z - that.z, cylSize)
}

object NormalCoords {
  def apply(vec: Vector3d) = new NormalCoords(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double) = new NormalCoords(_x, _y, _z)
}

/** NormalCoords with z axis wrapped around a cylinder. The y axis is perpendicular to the x and z axes and also exponential */
object CylCoords {
  def apply(vec: Vector3d, cylSize: CylinderSize) = new CylCoords(vec.x, vec.y, vec.z, cylSize)
  def apply(_x: Double, _y: Double, _z: Double, cylSize: CylinderSize) = new CylCoords(_x, _y, _z, cylSize)
}

/** CylCoords with the x axis rotated 30 deg */
object SkewCylCoords {
  def apply(vec: Vector3d, cylSize: CylinderSize) = new SkewCylCoords(vec.x, vec.y, vec.z, cylSize)
  def apply(_x: Double, _y: Double, _z: Double, cylSize: CylinderSize) = new SkewCylCoords(_x, _y, _z, cylSize)
}

/** SkewCylCoords with different axis scale */
object BlockCoords {
  def apply(vec: Vector3d, cylSize: CylinderSize) = new BlockCoords(vec.x, vec.y, vec.z, cylSize)
  def apply(_x: Double, _y: Double, _z: Double, cylSize: CylinderSize) = new BlockCoords(_x, _y, _z, cylSize)
  def apply(c: BlockRelWorld, cylSize: CylinderSize) = new BlockCoords(c.x, c.y, c.z, cylSize)
}

/** BlockCoords with x and z replaced with how far along to x and z axes the vector reaches. y = 0 in the middle of the block. */
object PlaceInBlockCoords {
  def apply(vec: Vector3d, cylSize: CylinderSize) = new PlaceInBlockCoords(vec.x, vec.y, vec.z, cylSize)
  def apply(_x: Double, _y: Double, _z: Double, cylSize: CylinderSize) = new PlaceInBlockCoords(_x, _y, _z, cylSize)
}
