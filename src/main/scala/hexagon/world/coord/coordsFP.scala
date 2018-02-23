package hexagon.world.coord

import hexagon.world.storage.World
import org.joml.Vector3d

sealed abstract class AbstractCoords[T <: AbstractCoords[T]](val x: Double, val y: Double, val z: Double) {
  def +(that: T): T
  def -(that: T): T
  def into(vec: Vector3d): Vector3d = vec.set(x, y, z)
  def toVector3d: Vector3d = into(new Vector3d)

  override def toString: String = f"($x%.3f, $y%.3f, $z%.3f)"
}

class NormalCoords(_x: Double, _y: Double, _z: Double) extends AbstractCoords[NormalCoords](_x, _y, _z) {
  def toCylCoord(reference: NormalCoords): CylCoords = ???
  def toSkewCylCoord(reference: NormalCoords): SkewCylCoords = toCylCoord(reference).toSkewCylCoord
  def toBlockCoord(reference: NormalCoords): BlockCoords = toCylCoord(reference).toBlockCoord

  def +(that: NormalCoords) = NormalCoords(x + that.x, y + that.y, z + that.z)
  def -(that: NormalCoords) = NormalCoords(x - that.x, y - that.y, z - that.z)
}

class CylCoords(_x: Double, _y: Double, _z: Double, val world: World, fixZ: Boolean = true)
  extends AbstractCoords[CylCoords](_x, _y, if (fixZ) CoordUtils.fitZ(_z, world.circumference) else _z) {

  def toNormalCoord(ref: CylCoords): NormalCoords = {
    val mult = math.exp((this.y - ref.y) / world.radius)
    val v = (this.z - ref.z) / CoordUtils.y60 * world.hexAngle
    val z = math.sin(v)
    val y = math.cos(v)

    val scale = world.radius // / math.sqrt(z * z + y * y)
    NormalCoords((this.x - ref.x) * mult, y * scale * mult - world.radius, z * scale * mult)
  }
  def toSkewCylCoord: SkewCylCoords = new SkewCylCoords(x / CoordUtils.y60, y, z - x * 0.5 / CoordUtils.y60, world, fixZ)
  def toBlockCoord: BlockCoords = toSkewCylCoord.toBlockCoord

  def +(that: CylCoords) = CylCoords(x + that.x, y + that.y, z + that.z, world)
  def -(that: CylCoords) = CylCoords(x - that.x, y - that.y, z - that.z, world)

  def distanceSq(c: CylCoords): Double = {
    val dx = x - c.x
    val dy = y - c.y
    val dz1 = CoordUtils.fitZ(z - c.z, world.circumference)
    val dz = math.min(dz1, world.circumference - dz1)
    dx * dx + dy * dy + dz * dz
  }
}

class SkewCylCoords(_x: Double, _y: Double, _z: Double, val world: World, fixZ: Boolean = true)
  extends AbstractCoords[SkewCylCoords](_x, _y, if (fixZ) CoordUtils.fitZ(_z, world.circumference) else _z) {

  def toNormalCoord(reference: CylCoords): NormalCoords = toCylCoord.toNormalCoord(reference)
  def toCylCoord: CylCoords = new CylCoords(x * CoordUtils.y60, y, z + x * 0.5, world, fixZ)
  def toBlockCoord: BlockCoords = new BlockCoords(x / CoordUtils.y60, y / 0.5, z / CoordUtils.y60, world, fixZ)

  def +(that: SkewCylCoords) = SkewCylCoords(x + that.x, y + that.y, z + that.z, world)
  def -(that: SkewCylCoords) = SkewCylCoords(x - that.x, y - that.y, z - that.z, world)
}

class BlockCoords(_x: Double, _y: Double, _z: Double, val world: World, fixZ: Boolean = true)
  extends AbstractCoords[BlockCoords](_x, _y, if (fixZ) CoordUtils.fitZ(_z, world.totalSize) else _z) {

  def toNormalCoord(reference: CylCoords): NormalCoords = toSkewCylCoord.toNormalCoord(reference)
  def toCylCoord: CylCoords = toSkewCylCoord.toCylCoord
  def toSkewCylCoord: SkewCylCoords = new SkewCylCoords(x * CoordUtils.y60, y * 0.5, z * CoordUtils.y60, world, fixZ)
  def toPlaceInBlockCoord: PlaceInBlockCoords = PlaceInBlockCoords(x + 0.5 * z, y - 0.5, z + 0.5 * x, world)

  def +(that: BlockCoords) = BlockCoords(x + that.x, y + that.y, z + that.z, world)
  def -(that: BlockCoords) = BlockCoords(x - that.x, y - that.y, z - that.z, world)
}

class PlaceInBlockCoords(_x: Double, _y: Double, _z: Double, world: World)
  extends AbstractCoords[PlaceInBlockCoords](_x, _y, _z) {

  def toBlockCoord: BlockCoords = BlockCoords((x - 0.5 * z) * 4 / 3, y + 0.5, (z - 0.5 * x) * 4 / 3, world)

  def +(that: PlaceInBlockCoords) = PlaceInBlockCoords(x + that.x, y + that.y, z + that.z, world)
  def -(that: PlaceInBlockCoords) = PlaceInBlockCoords(x - that.x, y - that.y, z - that.z, world)
}

object NormalCoords {
  def apply(vec: Vector3d) = new NormalCoords(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double) = new NormalCoords(_x, _y, _z)
}

/** NormalCoords with z axis wrapped around a cylinder. The y axis is perpendicular to the x and z axes and also logarithmic */
object CylCoords {
  def apply(vec: Vector3d, world: World) = new CylCoords(vec.x, vec.y, vec.z, world)
  def apply(_x: Double, _y: Double, _z: Double, world: World) = new CylCoords(_x, _y, _z, world)
}

/** CylCoords with the x axis rotated 30 deg */
object SkewCylCoords {
  def apply(vec: Vector3d, world: World) = new SkewCylCoords(vec.x, vec.y, vec.z, world)
  def apply(_x: Double, _y: Double, _z: Double, world: World) = new SkewCylCoords(_x, _y, _z, world)
}

/** SkewCylCoords with different axis scale */
object BlockCoords {
  def apply(vec: Vector3d, world: World) = new BlockCoords(vec.x, vec.y, vec.z, world)
  def apply(_x: Double, _y: Double, _z: Double, world: World) = new BlockCoords(_x, _y, _z, world)
  def apply(c: BlockRelWorld, world: World) = new BlockCoords(c.x, c.y, c.z, world)
}

/** BlockCoords with x and z replaced with how far along to x and z axes the vector reaches. y = 0 in the middle of the block. */
object PlaceInBlockCoords {
  def apply(vec: Vector3d, world: World) = new PlaceInBlockCoords(vec.x, vec.y, vec.z, world)
  def apply(_x: Double, _y: Double, _z: Double, world: World) = new PlaceInBlockCoords(_x, _y, _z, world)
}
