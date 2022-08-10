package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize

object ChunkRelWorld {
  def apply(X: Long, Y: Int, Z: Int)(implicit cylSize: CylinderSize): ChunkRelWorld =
    ChunkRelWorld((X & 0xfffff) << 32 | (Z & cylSize.ringSizeMask) << 12 | (Y & 0xfff))

  def apply(chunk: ChunkRelColumn, column: ColumnRelWorld): ChunkRelWorld = ChunkRelWorld(
    column.value << 12L | chunk.value
  )

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

case class ChunkRelWorld(value: Long) extends AnyVal {
  // XXXXXZZZZZYYY
  def getChunkRelColumn: ChunkRelColumn = ChunkRelColumn((value & 0xfff).toInt)
  def getColumnRelWorld: ColumnRelWorld = ColumnRelWorld(value >>> 12)

  def X: Int = (value >> 20).toInt >> 12
  def Z: Int = value.toInt >> 12
  def Y: Int = (value << 20).toInt >> 20

  def neighbors(implicit cylSize: CylinderSize): Seq[ChunkRelWorld] =
    ChunkRelWorld.neighborOffsets.map(offset)

  def extendedNeighbors(radius: Int)(implicit cylSize: CylinderSize): Seq[ChunkRelWorld] = for {
    y <- -radius to radius
    z <- -radius to radius
    x <- -radius to radius
  } yield offset(x, y, z)

  def offset(t: Offset)(implicit cylSize: CylinderSize): ChunkRelWorld = offset(t.dx, t.dy, t.dz)
  def offset(dx: Int, dy: Int, dz: Int)(implicit cylSize: CylinderSize): ChunkRelWorld =
    ChunkRelWorld(X + dx, Y + dy, Z + dz)

  override def toString: String = s"($X, $Y, $Z)"
}
