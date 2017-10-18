package hexagon.world.coord

import hexagon.world.storage.World
import org.joml.Vector3d

sealed abstract class AbstractCoord[T <: AbstractCoord[T]](val x: Double, val y: Double, val z: Double) {
  def +(that: T): T
  def -(that: T): T
  def into(vec: Vector3d): Vector3d = vec.set(x, y, z)
  def toVector3d: Vector3d = into(new Vector3d)

  override def toString: String = f"($x%.3f, $y%.3f, $z%.3f)"
}

class NormalCoord(_x: Double, _y: Double, _z: Double) extends AbstractCoord[NormalCoord](_x, _y, _z) {
  def toCylCoord(reference: NormalCoord): CylCoord = ???
  def toSkewCylCoord(reference: NormalCoord): SkewCylCoord = toCylCoord(reference).toSkewCylCoord
  def toBlockCoord(reference: NormalCoord): BlockCoord = toCylCoord(reference).toBlockCoord

  def +(that: NormalCoord) = NormalCoord(x + that.x, y + that.y, z + that.z)
  def -(that: NormalCoord) = NormalCoord(x - that.x, y - that.y, z - that.z)
}

class CylCoord(_x: Double, _y: Double, _z: Double, val world: World, fixZ: Boolean = true)
  extends AbstractCoord[CylCoord](_x, _y, if (fixZ) CoordUtils.fitZ(_z, world.circumference) else _z) {

  def toNormalCoord(ref: CylCoord): NormalCoord = {
    val mult = math.exp((this.y - ref.y) / world.radius)
    val v = (this.z - ref.z) / CoordUtils.y60 * world.hexAngle
    val z = math.sin(v)
    val y = math.cos(v)

    val scale = world.radius // / math.sqrt(z * z + y * y)
    NormalCoord((this.x - ref.x) * mult, y * scale * mult - world.radius, z * scale * mult)
  }
  def toSkewCylCoord: SkewCylCoord = new SkewCylCoord(x / CoordUtils.y60, y, z - x * 0.5 / CoordUtils.y60, world, fixZ)
  def toBlockCoord: BlockCoord = toSkewCylCoord.toBlockCoord

  def +(that: CylCoord) = CylCoord(x + that.x, y + that.y, z + that.z, world)
  def -(that: CylCoord) = CylCoord(x - that.x, y - that.y, z - that.z, world)

  def distanceSq(c: CylCoord): Double = {
    val dx = x - c.x
    val dy = y - c.y
    val dz1 = CoordUtils.fitZ(z - c.z, world.circumference)
    val dz = math.min(dz1, world.circumference - dz1)
    dx * dx + dy * dy + dz * dz
  }
}

class SkewCylCoord(_x: Double, _y: Double, _z: Double, val world: World, fixZ: Boolean = true)
  extends AbstractCoord[SkewCylCoord](_x, _y, if (fixZ) CoordUtils.fitZ(_z, world.circumference) else _z) {

  def toNormalCoord(reference: CylCoord): NormalCoord = toCylCoord.toNormalCoord(reference)
  def toCylCoord: CylCoord = new CylCoord(x * CoordUtils.y60, y, z + x * 0.5, world, fixZ)
  def toBlockCoord: BlockCoord = new BlockCoord(x / CoordUtils.y60, y / 0.5, z / CoordUtils.y60, world, fixZ)

  def +(that: SkewCylCoord) = SkewCylCoord(x + that.x, y + that.y, z + that.z, world)
  def -(that: SkewCylCoord) = SkewCylCoord(x - that.x, y - that.y, z - that.z, world)
}

class BlockCoord(_x: Double, _y: Double, _z: Double, val world: World, fixZ: Boolean = true)
  extends AbstractCoord[BlockCoord](_x, _y, if (fixZ) CoordUtils.fitZ(_z, world.totalSize) else _z) {

  def toNormalCoord(reference: CylCoord): NormalCoord = toSkewCylCoord.toNormalCoord(reference)
  def toCylCoord: CylCoord = toSkewCylCoord.toCylCoord
  def toSkewCylCoord: SkewCylCoord = new SkewCylCoord(x * CoordUtils.y60, y * 0.5, z * CoordUtils.y60, world, fixZ)
  def toPlaceInBlockCoord: PlaceInBlockCoord = PlaceInBlockCoord(x + 0.5 * z, y - 0.5, z + 0.5 * x, world)

  def +(that: BlockCoord) = BlockCoord(x + that.x, y + that.y, z + that.z, world)
  def -(that: BlockCoord) = BlockCoord(x - that.x, y - that.y, z - that.z, world)
}

class PlaceInBlockCoord(_x: Double, _y: Double, _z: Double, world: World)
  extends AbstractCoord[PlaceInBlockCoord](_x, _y, _z) {

  def toBlockCoord: BlockCoord = BlockCoord((x - 0.5 * z) * 4 / 3, y + 0.5, (z - 0.5 * x) * 4 / 3, world)

  def +(that: PlaceInBlockCoord) = PlaceInBlockCoord(x + that.x, y + that.y, z + that.z, world)
  def -(that: PlaceInBlockCoord) = PlaceInBlockCoord(x - that.x, y - that.y, z - that.z, world)
}

object NormalCoord {
  def apply(vec: Vector3d) = new NormalCoord(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double) = new NormalCoord(_x, _y, _z)
}

/** NormalCoord with z axis wrapped around a cylinder. The y axis is perp. to the x and z axes and also logarithmic */
object CylCoord {
  def apply(vec: Vector3d, world: World) = new CylCoord(vec.x, vec.y, vec.z, world)
  def apply(_x: Double, _y: Double, _z: Double, world: World) = new CylCoord(_x, _y, _z, world)
}

/** CylCoord with the x axis rotated 30 deg */
object SkewCylCoord {
  def apply(vec: Vector3d, world: World) = new SkewCylCoord(vec.x, vec.y, vec.z, world)
  def apply(_x: Double, _y: Double, _z: Double, world: World) = new SkewCylCoord(_x, _y, _z, world)
}

/** SkewCylCoord with different axis scale */
object BlockCoord {
  def apply(vec: Vector3d, world: World) = new BlockCoord(vec.x, vec.y, vec.z, world)
  def apply(_x: Double, _y: Double, _z: Double, world: World) = new BlockCoord(_x, _y, _z, world)
  def apply(c: BlockRelWorld, world: World) = new BlockCoord(c.x, c.y, c.z, world)
}

/** BlockCoord with x and z replaced with how far along to x and z axes the vector reaches. y = 0 in the middle of the block. */
object PlaceInBlockCoord {
  def apply(vec: Vector3d, world: World) = new PlaceInBlockCoord(vec.x, vec.y, vec.z, world)
  def apply(_x: Double, _y: Double, _z: Double, world: World) = new PlaceInBlockCoord(_x, _y, _z, world)
}
