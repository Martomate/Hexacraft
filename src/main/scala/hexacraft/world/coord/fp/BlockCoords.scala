package hexacraft.world.coord.fp

import hexacraft.math.MathUtils
import hexacraft.world.CylinderSize
import hexacraft.world.coord.integer.BlockRelWorld

class BlockCoords private (_x: Double, _y: Double, _z: Double) extends AbstractCoords[BlockCoords](_x, _y, _z) {
  def toNormalCoords(reference: CylCoords)(using CylinderSize): NormalCoords = toSkewCylCoords.toNormalCoords(reference)
  def toCylCoords(using CylinderSize): CylCoords = toSkewCylCoords.toCylCoords
  def toSkewCylCoords(using CylinderSize): SkewCylCoords =
    SkewCylCoords(x * CylinderSize.y60, y * 0.5, z * CylinderSize.y60)

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
    def toSkewCylCoordsOffset: SkewCylCoords.Offset =
      SkewCylCoords.Offset(x * CylinderSize.y60, y * 0.5, z * CylinderSize.y60)

    override def offset(dx: Double, dy: Double, dz: Double): BlockCoords.Offset =
      BlockCoords.Offset(x + dx, y + dy, z + dz)

    def offset(c: BlockCoords.Offset): BlockCoords.Offset = this + c
  }
}
