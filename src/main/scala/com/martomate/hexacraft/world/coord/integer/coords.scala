package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.world.CylinderSize
import org.joml.Vector2d

abstract class AbstractIntegerCoords[T](val value: T) {
  override def equals(o: Any): Boolean = o match {
    case c: AbstractIntegerCoords[T] => c.value == value
    case _ => false
  }
}

object BlockRelChunk {
  def apply(x: Int, y: Int, z: Int, cylSize: CylinderSize): BlockRelChunk = BlockRelChunk((x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf), cylSize)
}
case class BlockRelChunk(private val _value: Int, cylSize: CylinderSize) extends AbstractIntegerCoords(_value) { // xyz
  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte

  def offset(x: Int, y: Int, z: Int): BlockRelChunk = BlockRelChunk(cx + x, cy + y, cz + z, cylSize)

  override def toString: String = s"($cx, $cy, $cz)"
}

object BlockRelColumn {
  def apply(Y: Int, x: Int, y: Int, z: Int, cylSize: CylinderSize): BlockRelColumn = BlockRelColumn((Y & 0xfff) << 12 | ((x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf)), cylSize)
  def apply(block: BlockRelChunk, chunk: ChunkRelColumn): BlockRelColumn = BlockRelColumn(chunk.value << 12 | block.value, chunk.cylSize)
}
case class BlockRelColumn(private val _value: Int, cylSize: CylinderSize) extends AbstractIntegerCoords(_value) { // YYYxyz
  def getBlockRelChunk = BlockRelChunk(value & 0xfff, cylSize)
  def getChunkRelColumn = ChunkRelColumn(value >> 12 & 0xfff, cylSize)

  def Y: Int = value << 8 >> 20
  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte
  def y: Int = Y * 16 + cy
}

case class ChunkRelColumn(private val _value: Int, cylSize: CylinderSize) extends AbstractIntegerCoords(_value & 0xfff) { // YYY
  def Y: Int = value << 20 >> 20
}

object BlockRelWorld {
  def apply(X: Long, Y: Int, Z: Long, x: Int, y: Int, z: Int, cylSize: CylinderSize): BlockRelWorld =
    BlockRelWorld((X & 0xfffff) << 44L | (Z & cylSize.ringSizeMask) << 24 | (Y & 0xfff) << 12 | (x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf), cylSize)
  def apply(x: Int, y: Int, z: Int, cylSize: CylinderSize): BlockRelWorld = BlockRelWorld(x >> 4, y >> 4, z >> 4, x & 15, y & 15, z & 15, cylSize)

  def apply(block: BlockRelChunk, chunk: ChunkRelWorld): BlockRelWorld = BlockRelWorld(chunk.value << 12 | block.value, chunk.cylSize)
  def apply(block: BlockRelColumn, column: ColumnRelWorld): BlockRelWorld = BlockRelWorld(column.value << 24L | block.value, column.cylSize)
  def apply(i: Int, j: Int, k: Int, chunk: ChunkRelWorld): BlockRelWorld = BlockRelWorld(chunk.X * 16 + i, chunk.Y * 16 + j, chunk.Z * 16 + k, chunk.cylSize)
}
case class BlockRelWorld(private val _value: Long, cylSize: CylinderSize) extends AbstractIntegerCoords(_value) { // XXXXXZZZZZYYYxyz
  def getBlockRelChunk = BlockRelChunk((value & 0xfff).toInt, cylSize)
  def getBlockRelColumn = BlockRelColumn((value & 0xffffff).toInt, cylSize)
  def getChunkRelColumn = ChunkRelColumn((value >>> 12 & 0xfff).toInt, cylSize)
  def getChunkRelWorld = ChunkRelWorld(value >>> 12, cylSize)
  def getColumnRelWorld = ColumnRelWorld(value >>> 24, cylSize)

  def offset(xx: Int, yy: Int, zz: Int) = BlockRelWorld(x + xx, y + yy, z + zz, cylSize)

  def X: Int = (value >> 32).toInt >> 12
  def Z: Int = (value >> 12).toInt >> 12
  def Y: Int = (value << 8).toInt >> 20
  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte
  def x: Int = X << 4 | cx
  def y: Int = Y << 4 | cy
  def z: Int = Z << 4 | cz

  override def toString = s"($x, $y, $z)"
}

object ChunkRelWorld {
  def apply(X: Long, Y: Int, Z: Int, cylSize: CylinderSize): ChunkRelWorld = ChunkRelWorld((X & 0xfffff) << 32 | (Z & cylSize.ringSizeMask) << 12 | (Y & 0xfff), cylSize)

  def apply(chunk: ChunkRelColumn, column: ColumnRelWorld): ChunkRelWorld = ChunkRelWorld(column.value << 12L | chunk.value, column.cylSize)

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
case class ChunkRelWorld(private val _value: Long, cylSize: CylinderSize) extends AbstractIntegerCoords(_value) {
  // XXXXXZZZZZYYY
  def getChunkRelColumn = ChunkRelColumn((value & 0xfff).toInt, cylSize)
  def getColumnRelWorld = ColumnRelWorld(value >>> 12, cylSize)

  def X: Int = (value >> 20).toInt >> 12
  def Z: Int = value.toInt >> 12
  def Y: Int = (value << 20).toInt >> 20

  def neighbors: Seq[ChunkRelWorld] = ChunkRelWorld.neighborOffsets.map(d => offset(d._1, d._2, d._3))
  def offset(x: Int, y: Int, z: Int): ChunkRelWorld = ChunkRelWorld(X + x, Y + y, Z + z, cylSize)

  override def toString: String = s"($X, $Y, $Z)"
}

object ColumnRelWorld {
  def apply(X: Long, Z: Int, cylSize: CylinderSize): ColumnRelWorld = ColumnRelWorld((X & 0xfffff) << 20 | (Z & cylSize.ringSizeMask), cylSize)
}
case class ColumnRelWorld(private val _value: Long, cylSize: CylinderSize) extends AbstractIntegerCoords(_value) { // XXXXXZZZZZ
  def X: Int = (value >> 8).toInt >> 12
  def Z: Int = (value << 12).toInt >> 12

  def offset(x: Int, z: Int): ColumnRelWorld = ColumnRelWorld(X + x, Z + z, cylSize)

  def distSq(origin: Vector2d): Double = {
    val dx = this.X - origin.x + 0.5
    val dz1 = math.abs(this.Z - origin.y + 0.5 + dx * 0.5f)
    val dz2 = math.abs(dz1 - cylSize.ringSize)
    val dz = math.min(dz1, dz2)
    dx * dx + dz * dz
  }
}
