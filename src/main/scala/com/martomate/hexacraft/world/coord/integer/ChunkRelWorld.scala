package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.Offset

object ChunkRelWorld {
  def apply(X: Long, Y: Int, Z: Int)(implicit cylSize: CylinderSize): ChunkRelWorld = ChunkRelWorld((X & 0xfffff) << 32 | (Z & cylSize.ringSizeMask) << 12 | (Y & 0xfff))

  def apply(chunk: ChunkRelColumn, column: ColumnRelWorld): ChunkRelWorld = ChunkRelWorld(column.value << 12L | chunk.value)(column.cylSize)

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

case class ChunkRelWorld private (value: Long)(implicit val cylSize: CylinderSize) {
  // XXXXXZZZZZYYY
  def getChunkRelColumn = ChunkRelColumn((value & 0xfff).toInt)
  def getColumnRelWorld = ColumnRelWorld(value >>> 12)

  def X: Int = (value >> 20).toInt >> 12
  def Z: Int = value.toInt >> 12
  def Y: Int = (value << 20).toInt >> 20

  def neighbors: Seq[ChunkRelWorld] = ChunkRelWorld.neighborOffsets.map(offset)

  def extendedNeighbors(radius: Int): Seq[ChunkRelWorld] = for {
    y <- -radius to radius
    z <- -radius to radius
    x <- -radius to radius
  } yield offset(x, y, z)

  def offset(t: Offset): ChunkRelWorld = offset(t.dx, t.dy, t.dz)
  def offset(x: Int, y: Int, z: Int): ChunkRelWorld = ChunkRelWorld(X + x, Y + y, Z + z)

  override def toString: String = s"($X, $Y, $Z)"
}
