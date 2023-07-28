package hexacraft.world.coord.fp

import hexacraft.world.CylinderSize
import org.joml.{Vector3d, Vector3f}

private[fp] abstract class AbstractCoords[T <: AbstractCoords[T]](
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
  private[fp] abstract class Offset[T <: AbstractCoords.Offset[T]](
      val x: Double,
      val y: Double,
      val z: Double
  ):
    def offset(dx: Double, dy: Double, dz: Double): T

    def +(that: T): T = offset(that.x, that.y, that.z)
    def -(that: T): T = offset(-that.x, -that.y, -that.z)

    def into(vec: Vector3f): Vector3f = vec.set(x.toFloat, y.toFloat, z.toFloat)
    def toVector3f: Vector3f = into(new Vector3f)
    def into(vec: Vector3d): Vector3d = vec.set(x, y, z)
    def toVector3d: Vector3d = into(new Vector3d)

    override def toString: String = f"($x%.3f, $y%.3f, $z%.3f)"
