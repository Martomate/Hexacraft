package hexacraft.world.coord.integer

import hexacraft.math.{Int12, Int20}
import hexacraft.world.CylinderSize

object ChunkRelWorld {
  def apply(X: Long, Y: Int, Z: Int)(using cylSize: CylinderSize): ChunkRelWorld =
    ChunkRelWorld((X & 0xfffff) << 32 | (Z & cylSize.ringSizeMask) << 12 | (Y & 0xfff))

  def apply(chunk: Int12, column: ColumnRelWorld): ChunkRelWorld =
    ChunkRelWorld(column.value << 12L | chunk.repr.toInt)

  val neighborOffsets: Seq[Offset] = Seq(
    Offset(0, 1, 0),
    Offset(1, 0, 0),
    Offset(0, 0, 1),
    Offset(-1, 0, 1),
    Offset(0, -1, 0),
    Offset(-1, 0, 0),
    Offset(0, 0, -1),
    Offset(1, 0, -1)
  )
}

case class ChunkRelWorld(value: Long) extends AnyVal { // XXXXXZZZZZYYY
  def getColumnRelWorld: ColumnRelWorld = ColumnRelWorld(value >>> 12)

  def X: Int20 = Int20.truncate(value >> 32)
  def Z: Int20 = Int20.truncate(value >> 12)
  def Y: Int12 = Int12.truncate(value)

  def neighbors(using CylinderSize): Seq[ChunkRelWorld] =
    ChunkRelWorld.neighborOffsets.map(offset)

  def extendedNeighbors(radius: Int)(using CylinderSize): Seq[ChunkRelWorld] = for {
    y <- -radius to radius
    z <- -radius to radius
    x <- -radius to radius
  } yield offset(x, y, z)

  def offset(t: Offset)(using CylinderSize): ChunkRelWorld = offset(t.dx, t.dy, t.dz)
  def offset(dx: Int, dy: Int, dz: Int)(using CylinderSize): ChunkRelWorld =
    ChunkRelWorld(X.toInt + dx, Y.toInt + dy, Z.toInt + dz)

  override def toString: String = s"(${X.toInt}, ${Y.toInt}, ${Z.toInt})"
}
