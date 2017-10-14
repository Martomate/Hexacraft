package hexagon.world.coord

import hexagon.Camera
import hexagon.world.storage.World
import org.joml.Vector3d

sealed abstract class AbstractCoord[T <: AbstractCoord[T]](val x: Double, val y: Double, val z: Double) {
  def +(that: T): T
  def -(that: T): T
  def into(vec: Vector3d): Vector3d = vec.set(x, y, z)
  def toVector3d: Vector3d = into(new Vector3d)

  override def toString(): String = f"($x%.3f, $y%.3f, $z%.3f)"
}

class NormalCoord(_x: Double, _y: Double, _z: Double) extends AbstractCoord[NormalCoord](_x, _y, _z) {
  def toCylCoord(reference: NormalCoord): CylCoord = ???
  def toSkewCylCoord(reference: NormalCoord): SkewCylCoord = toCylCoord(reference).toSkewCylCoord()
  def toBlockCoord(reference: NormalCoord): BlockCoord = toCylCoord(reference).toBlockCoord()

  def +(that: NormalCoord) = NormalCoord(x + that.x, y + that.y, z + that.z)
  def -(that: NormalCoord) = NormalCoord(x - that.x, y - that.y, z - that.z)
}

class CylCoord(_x: Double, _y: Double, _z: Double, fixZ: Boolean = true)
  extends AbstractCoord[CylCoord](_x, _y, if (fixZ) CoordUtils.fitZ(_z, World.circumference) else _z) {

  def toNormalCoord(ref: CylCoord): NormalCoord = {
    val mult = math.exp((this.y - ref.y) / World.radius)
    val v = (this.z - ref.z) / CoordUtils.y60 * World.hexAngle
    val z = math.sin(v)
    val y = math.cos(v)

    val scale = World.radius // / math.sqrt(z * z + y * y)
    NormalCoord((this.x - ref.x) * mult, y * scale * mult - World.radius, z * scale * mult)
  }
  def toSkewCylCoord(): SkewCylCoord = new SkewCylCoord(x / CoordUtils.y60, y, z - x * 0.5 / CoordUtils.y60, fixZ)
  def toBlockCoord(): BlockCoord = toSkewCylCoord().toBlockCoord()

  def +(that: CylCoord) = CylCoord(x + that.x, y + that.y, z + that.z)
  def -(that: CylCoord) = CylCoord(x - that.x, y - that.y, z - that.z)

  def distanceSq(c: CylCoord) = {
    val dx = x - c.x
    val dy = y - c.y
    val dz1 = CoordUtils.fitZ(z - c.z, World.circumference)
    val dz = math.min(dz1, World.circumference - dz1)
    dx * dx + dy * dy + dz * dz
  }
}

class SkewCylCoord(_x: Double, _y: Double, _z: Double, fixZ: Boolean = true)
  extends AbstractCoord[SkewCylCoord](_x, _y, if (fixZ) CoordUtils.fitZ(_z, World.circumference) else _z) {

  def toNormalCoord(reference: CylCoord): NormalCoord = toCylCoord().toNormalCoord(reference)
  def toCylCoord(): CylCoord = new CylCoord(x * CoordUtils.y60, y, z + x * 0.5, fixZ)
  def toBlockCoord(): BlockCoord = new BlockCoord(x / CoordUtils.y60, y / 0.5, z / CoordUtils.y60, fixZ)

  def +(that: SkewCylCoord) = SkewCylCoord(x + that.x, y + that.y, z + that.z)
  def -(that: SkewCylCoord) = SkewCylCoord(x - that.x, y - that.y, z - that.z)
}

class BlockCoord(_x: Double, _y: Double, _z: Double, fixZ: Boolean = true)
  extends AbstractCoord[BlockCoord](_x, _y, if (fixZ) CoordUtils.fitZ(_z, World.totalSize) else _z) {

  def toNormalCoord(reference: CylCoord): NormalCoord = toSkewCylCoord().toNormalCoord(reference)
  def toCylCoord(): CylCoord = toSkewCylCoord().toCylCoord()
  def toSkewCylCoord(): SkewCylCoord = new SkewCylCoord(x * CoordUtils.y60, y * 0.5, z * CoordUtils.y60, fixZ)
  def toPlaceInBlockCoord(): PlaceInBlockCoord = PlaceInBlockCoord(x + 0.5 * z, y - 0.5, z + 0.5 * x)

  def +(that: BlockCoord) = BlockCoord(x + that.x, y + that.y, z + that.z)
  def -(that: BlockCoord) = BlockCoord(x - that.x, y - that.y, z - that.z)
}

class PlaceInBlockCoord(_x: Double, _y: Double, _z: Double)
  extends AbstractCoord[PlaceInBlockCoord](_x, _y, _z) {

  def toBlockCoord(): BlockCoord = BlockCoord((x - 0.5 * z) * 4 / 3, y + 0.5, (z - 0.5 * x) * 4 / 3)

  def +(that: PlaceInBlockCoord) = PlaceInBlockCoord(x + that.x, y + that.y, z + that.z)
  def -(that: PlaceInBlockCoord) = PlaceInBlockCoord(x - that.x, y - that.y, z - that.z)
}

object NormalCoord {
  def apply(vec: Vector3d) = new NormalCoord(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double) = new NormalCoord(_x, _y, _z)
}

/** NormalCoord with z axis wrapped around a cylinder. The y axis is perp. to the x and z axes and also logarithmic */
object CylCoord {
  def apply(vec: Vector3d) = new CylCoord(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double) = new CylCoord(_x, _y, _z)
}

/** CylCoord with the x axis rotated 30 deg */
object SkewCylCoord {
  def apply(vec: Vector3d) = new SkewCylCoord(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double) = new SkewCylCoord(_x, _y, _z)
}

/** SkewCylCoord with different axis scale */
object BlockCoord {
  def apply(vec: Vector3d) = new BlockCoord(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double) = new BlockCoord(_x, _y, _z)
  def apply(c: BlockRelWorld) = new BlockCoord(c.x, c.y, c.z)
}

/** BlockCoord with x and z replaced with how far along to x and z axes the vector reaches. y = 0 in the middle of the block. */
object PlaceInBlockCoord {
  def apply(vec: Vector3d) = new PlaceInBlockCoord(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double) = new PlaceInBlockCoord(_x, _y, _z)
}
