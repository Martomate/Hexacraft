package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize

object ChunkRelWorld {
  def apply(X: Long, Y: Int, Z: Int)(implicit cylSize: CylinderSize): ChunkRelWorld = ChunkRelWorld((X & 0xfffff) << 32 | (Z & cylSize.ringSizeMask) << 12 | (Y & 0xfff))

  def apply(chunk: ChunkRelColumn, column: ColumnRelWorld): ChunkRelWorld = ChunkRelWorld(column.value << 12L | chunk.value)(column.cylSize)

  val neighborOffsets: Seq[(Int, Int, Int)] = Seq(
    (0, 1, 0),
    (1, 0, 0),
    (0, 0, 1),
    (-1, 0, 1),
    (0, -1, 0),
    (-1, 0, 0),
    (0, 0, -1),
    (1, 0, -1)
  )
}

case class ChunkRelWorld(private val _value: Long)(implicit val cylSize: CylinderSize) extends AbstractIntegerCoords(_value) {
  // XXXXXZZZZZYYY
  def getChunkRelColumn = ChunkRelColumn((value & 0xfff).toInt)
  def getColumnRelWorld = ColumnRelWorld(value >>> 12)

  def X: Int = (value >> 20).toInt >> 12
  def Z: Int = value.toInt >> 12
  def Y: Int = (value << 20).toInt >> 20

  def neighbors: Seq[ChunkRelWorld] = ChunkRelWorld.neighborOffsets.map(d => offset(d._1, d._2, d._3))
  def offset(x: Int, y: Int, z: Int): ChunkRelWorld = ChunkRelWorld(X + x, Y + y, Z + z)

  override def toString: String = s"($X, $Y, $Z)"
}