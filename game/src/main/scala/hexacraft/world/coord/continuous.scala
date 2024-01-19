package hexacraft.world.coord

import hexacraft.math.MathUtils
import hexacraft.world.CylinderSize

import org.joml.{Vector3d, Vector3f}

private[coord] abstract class AbstractCoords[T <: AbstractCoords[T]](
    val x: Double,
    val y: Double,
    val z: Double
) {
  def +(that: T)(using CylinderSize): T = offset(that.x, that.y, that.z)
  def -(that: T)(using CylinderSize): T = offset(-that.x, -that.y, -that.z)
  def offset(dx: Double, dy: Double, dz: Double)(using CylinderSize): T
  def into(vec: Vector3f): Vector3f = vec.set(x.toFloat, y.toFloat, z.toFloat)
  def toVector3f: Vector3f = into(new Vector3f)
  def into(vec: Vector3d): Vector3d = vec.set(x, y, z)
  def toVector3d: Vector3d = into(new Vector3d)

  override def toString: String = f"($x%.3f, $y%.3f, $z%.3f)"
}

object AbstractCoords:
  private[coord] abstract class Offset[T <: AbstractCoords.Offset[T]](
      val x: Double,
      val y: Double,
      val z: Double
  ) {
    def offset(dx: Double, dy: Double, dz: Double): T

    def +(that: T): T = offset(that.x, that.y, that.z)

    def -(that: T): T = offset(-that.x, -that.y, -that.z)

    def into(vec: Vector3f): Vector3f = vec.set(x.toFloat, y.toFloat, z.toFloat)

    def toVector3f: Vector3f = into(new Vector3f)

    def into(vec: Vector3d): Vector3d = vec.set(x, y, z)

    def toVector3d: Vector3d = into(new Vector3d)

    override def toString: String = f"($x%.3f, $y%.3f, $z%.3f)"
  }

class BlockCoords private (_x: Double, _y: Double, _z: Double) extends AbstractCoords[BlockCoords](_x, _y, _z) {
  def toNormalCoords(reference: CylCoords)(using CylinderSize): NormalCoords = toSkewCylCoords.toNormalCoords(reference)
  def toCylCoords(using CylinderSize): CylCoords = toSkewCylCoords.toCylCoords
  def toSkewCylCoords(using CylinderSize): SkewCylCoords = SkewCylCoords(
    x * CylinderSize.y60,
    y * 0.5,
    z * CylinderSize.y60
  )

  override def offset(dx: Double, dy: Double, dz: Double)(using CylinderSize): BlockCoords =
    BlockCoords(x + dx, y + dy, z + dz)

  def offset(c: BlockCoords)(using CylinderSize): BlockCoords = this + c
  def offset(c: BlockCoords.Offset)(using CylinderSize): BlockCoords = offset(c.x, c.y, c.z)
}

/** BlockCoords are like SkewCylCoords but with a different axis scale */
object BlockCoords {
  def apply(_x: Double, _y: Double, _z: Double)(using cylSize: CylinderSize): BlockCoords =
    new BlockCoords(_x, _y, MathUtils.fitZ(_z, cylSize.totalSize))

  def apply(c: BlockRelWorld)(using cylSize: CylinderSize): BlockCoords =
    new BlockCoords(c.x, c.y, MathUtils.fitZ(c.z, cylSize.totalSize))

  case class Offset(_x: Double, _y: Double, _z: Double) extends AbstractCoords.Offset[BlockCoords.Offset](_x, _y, _z) {
    def toCylCoordsOffset: CylCoords.Offset = toSkewCylCoordsOffset.toCylCoordsOffset
    def toSkewCylCoordsOffset: SkewCylCoords.Offset = SkewCylCoords.Offset(
      x * CylinderSize.y60,
      y * 0.5,
      z * CylinderSize.y60
    )

    override def offset(dx: Double, dy: Double, dz: Double): BlockCoords.Offset =
      BlockCoords.Offset(x + dx, y + dy, z + dz)

    def offset(c: BlockCoords.Offset): BlockCoords.Offset = this + c
  }
}

class CylCoords private (_x: Double, _y: Double, _z: Double) extends AbstractCoords[CylCoords](_x, _y, _z) {
  def toNormalCoords(ref: CylCoords)(using cylSize: CylinderSize): NormalCoords = {
    val mult = math.exp((this.y - ref.y) / cylSize.radius)
    val v = (this.z - ref.z) / CylinderSize.y60 * cylSize.hexAngle
    val z = math.sin(v)
    val y = math.cos(v)

    val scale = cylSize.radius // / math.sqrt(z * z + y * y)
    NormalCoords((this.x - ref.x) * mult, y * scale * mult - cylSize.radius, z * scale * mult)
  }

  def toSkewCylCoords(using CylinderSize): SkewCylCoords = SkewCylCoords(
    x / CylinderSize.y60,
    y,
    z - x * 0.5 / CylinderSize.y60
  )

  def toBlockCoords(using CylinderSize): BlockCoords = toSkewCylCoords.toBlockCoords

  def distanceSq(c: CylCoords)(using cylSize: CylinderSize): Double = {
    val dx = x - c.x
    val dy = y - c.y
    val dz1 = MathUtils.fitZ(z - c.z, cylSize.circumference)
    val dz = math.min(dz1, cylSize.circumference - dz1)
    dx * dx + dy * dy + dz * dz
  }

  def distanceXZSq(c: CylCoords)(using cylSize: CylinderSize): Double = {
    val dx = x - c.x
    val dz1 = MathUtils.fitZ(z - c.z, cylSize.circumference)
    val dz = math.min(dz1, cylSize.circumference - dz1)
    dx * dx + dz * dz
  }

  def angleXZ(c: CylCoords)(using cylSize: CylinderSize): Double = {
    val dx = c.x - x
    // dz: -circ / 2 until circ / 2
    val dz = MathUtils.fitZ(
      c.z - z + cylSize.circumference / 2,
      cylSize.circumference
    ) - cylSize.circumference / 2
    math.atan2(dz, dx)
  }

  override def offset(dx: Double, dy: Double, dz: Double)(using CylinderSize): CylCoords =
    CylCoords(x + dx, y + dy, z + dz)

  def offset(v: Vector3d)(using CylinderSize): CylCoords = offset(v.x, v.y, v.z)
  def offset(v: CylCoords.Offset)(using CylinderSize): CylCoords = offset(v.x, v.y, v.z)
}

/** NormalCoords with z axis wrapped around a cylinder. The y axis is perpendicular to the x and z
  * axes and also exponential
  */
object CylCoords {
  def apply(vec: Vector3d)(using cylSize: CylinderSize): CylCoords =
    new CylCoords(vec.x, vec.y, MathUtils.fitZ(vec.z, cylSize.circumference))

  def apply(_x: Double, _y: Double, _z: Double)(using cylSize: CylinderSize): CylCoords =
    new CylCoords(_x, _y, MathUtils.fitZ(_z, cylSize.circumference))

  case class Offset(_x: Double, _y: Double, _z: Double) extends AbstractCoords.Offset[CylCoords.Offset](_x, _y, _z) {
    def toSkewCylCoordsOffset: SkewCylCoords.Offset = SkewCylCoords.Offset(
      x / CylinderSize.y60,
      y,
      z - x * 0.5 / CylinderSize.y60
    )

    def toBlockCoordsOffset: BlockCoords.Offset = toSkewCylCoordsOffset.toBlockCoordsOffset

    override def offset(dx: Double, dy: Double, dz: Double): CylCoords.Offset =
      CylCoords.Offset(x + dx, y + dy, z + dz)

    def offset(v: Vector3d): CylCoords.Offset = offset(v.x, v.y, v.z)
  }
  object Offset:
    def apply(vec: Vector3d): Offset = Offset(vec.x, vec.y, vec.z)
}

class NormalCoords private (_x: Double, _y: Double, _z: Double) extends AbstractCoords[NormalCoords](_x, _y, _z) {
  def toCylCoords(reference: NormalCoords): CylCoords = ???

  def toSkewCylCoords(reference: NormalCoords)(using CylinderSize): SkewCylCoords =
    toCylCoords(reference).toSkewCylCoords
  def toBlockCoords(reference: NormalCoords)(using CylinderSize): BlockCoords = toCylCoords(reference).toBlockCoords

  override def offset(dx: Double, dy: Double, dz: Double)(using CylinderSize): NormalCoords =
    NormalCoords(x + dx, y + dy, z + dz)
}

object NormalCoords {
  def apply(vec: Vector3d) = new NormalCoords(vec.x, vec.y, vec.z)
  def apply(_x: Double, _y: Double, _z: Double) = new NormalCoords(_x, _y, _z)
}

class SkewCylCoords private (_x: Double, _y: Double, _z: Double)(using CylinderSize)
    extends AbstractCoords[SkewCylCoords](_x, _y, _z) {

  def toNormalCoords(reference: CylCoords): NormalCoords = toCylCoords.toNormalCoords(reference)

  def toCylCoords: CylCoords = CylCoords(
    x * CylinderSize.y60,
    y,
    z + x * 0.5
  )

  def toBlockCoords: BlockCoords = BlockCoords(
    x / CylinderSize.y60,
    y / 0.5,
    z / CylinderSize.y60
  )

  override def offset(dx: Double, dy: Double, dz: Double)(using CylinderSize): SkewCylCoords =
    SkewCylCoords(x + dx, y + dy, z + dz)

  def offset(v: SkewCylCoords.Offset)(using CylinderSize): SkewCylCoords = offset(v.x, v.y, v.z)
}

/** CylCoords with the x axis rotated 30 deg */
object SkewCylCoords {
  def apply(_x: Double, _y: Double, _z: Double)(using cylSize: CylinderSize) =
    new SkewCylCoords(_x, _y, MathUtils.fitZ(_z, cylSize.circumference))

  case class Offset(_x: Double, _y: Double, _z: Double)
      extends AbstractCoords.Offset[SkewCylCoords.Offset](_x, _y, _z) {
    def toCylCoordsOffset: CylCoords.Offset = CylCoords.Offset(
      x * CylinderSize.y60,
      y,
      z + x * 0.5
    )

    def toBlockCoordsOffset: BlockCoords.Offset = BlockCoords.Offset(
      x / CylinderSize.y60,
      y / 0.5,
      z / CylinderSize.y60
    )

    override def offset(dx: Double, dy: Double, dz: Double): SkewCylCoords.Offset =
      SkewCylCoords.Offset(x + dx, y + dy, z + dz)

    def offset(c: SkewCylCoords.Offset): SkewCylCoords.Offset = this + c
  }
}
