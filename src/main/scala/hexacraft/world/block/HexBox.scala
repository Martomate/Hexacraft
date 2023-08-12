package hexacraft.world.block

import hexacraft.math.geometry.{ConvexHull, OrthogonalProjection}
import hexacraft.util.MathUtils
import hexacraft.world.CylinderSize
import hexacraft.world.coord.CoordUtils
import hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import hexacraft.world.coord.integer.BlockRelWorld

import org.joml.Vector3dc

import scala.annotation.tailrec

/** radius is the big radius of the hexagon */
class HexBox(val radius: Float, val bottom: Float, val top: Float) {
  val smallRadius: Double = radius * CylinderSize.y60

  def scaledRadially(scale: Float): HexBox = new HexBox(radius * scale, bottom, top)

  def baseArea: Double = radius * radius * CylinderSize.y60 * 3

  def volume: Double = baseArea * (top - bottom)

  def projectedAreaInDirection(dir: Vector3dc): Double =
    val projection = OrthogonalProjection.inDirection(dir)
    val projectedVertices = vertices.map(v => projection.project(v.toVector3d))
    val polygon = ConvexHull.calculate(projectedVertices)
    polygon.area

  def vertices: IndexedSeq[CylCoords.Offset] = {
    // val ints = Seq(1, 2, 0, 3, 5, 4)

    for {
      s <- 0 to 1
      i <- 0 until 6
    } yield {
      val v = i * Math.PI / 3
      val x = Math.cos(v).toFloat
      val z = Math.sin(v).toFloat
      CylCoords.Offset(x * radius, (1 - s) * (top - bottom) + bottom, z * radius)
    }
  }

  /** Returns all blocks spaces that would intersect with this HexBox when placed at the given position */
  def cover(pos: CylCoords)(using CylinderSize): Seq[BlockRelWorld] =
    val yLo = math.floor((pos.y + this.bottom) * 2).toInt
    val yHi = math.floor((pos.y + this.top) * 2).toInt

    // TODO: improve this implementation to be more correct
    for
      y <- yLo to yHi
      dx <- -1 to 1
      dz <- -1 to 1
      if dx * dz != 1 // remove corners
    yield
      val origin = pos.toBlockCoords.offset(dx, 0, dz)
      CoordUtils.getEnclosingBlock(BlockCoords(origin.x, y, origin.z))._1
}

object HexBox {
  @tailrec
  def approximateVolumeOfIntersection(pos1: CylCoords, box1: HexBox, pos2: CylCoords, box2: HexBox): Double =
    if box1.radius < box2.radius
    then approximateVolumeOfIntersection(pos2, box2, pos1, box1)
    else
      val r1 = box1.radius
      val r2 = box2.radius
      val d = math.hypot(pos1.x - pos2.x, pos1.z - pos2.z)
      val a2 = box2.baseArea

      val baseArea = MathUtils.smoothstep(MathUtils.remap(d.toFloat, r1 - r2, r1 + r2, 1, 0)) * a2

      val t1 = pos1.y + box1.top
      val b1 = pos1.y + box1.bottom
      val t2 = pos2.y + box2.top
      val b2 = pos2.y + box2.bottom

      val height = math.max(0, math.min(t1, t2) - math.max(b1, b2))

      baseArea * height
}
