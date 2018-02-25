package hexacraft.world.coord

import hexacraft.world.storage.CylinderSize
import org.joml.Vector2d

object BlockRelChunk {
  def apply(x: Int, y: Int, z: Int, cylSize: CylinderSize): BlockRelChunk = BlockRelChunk((x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf), cylSize)
}
case class BlockRelChunk(value: Int, cylSize: CylinderSize) { // xyz

  def withChunk(chunk: ChunkRelColumn) = BlockRelColumn(chunk.value << 12 | value, cylSize)
  def withChunk(chunk: ChunkRelWorld) = BlockRelWorld(chunk.value << 12 | value, cylSize)

  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte
}

object BlockRelColumn {
  def apply(Y: Int, x: Int, y: Int, z: Int, cylSize: CylinderSize): BlockRelColumn = BlockRelColumn((Y & 0xfff) << 12 | ((x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf)), cylSize)
}
case class BlockRelColumn(value: Int, cylSize: CylinderSize) { // YYYxyz
  def withColumn(column: ColumnRelWorld) = BlockRelWorld(column.value << 24L | value, cylSize)

  def getBlockRelChunk = BlockRelChunk(value & 0xfff, cylSize)
  def getChunkRelColumn = ChunkRelColumn(value >> 12 & 0xfff, cylSize)

  def Y: Int = value << 8 >> 20
  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte
}

case class ChunkRelColumn(value: Int, cylSize: CylinderSize) { // YYY
  def withColumn(column: ColumnRelWorld) = ChunkRelWorld(column.value << 12L | value, cylSize)

  def Y: Int = value << 20 >> 20
}

object BlockRelWorld {
  def apply(X: Long, Y: Int, Z: Long, x: Int, y: Int, z: Int, cylSize: CylinderSize): BlockRelWorld =
    BlockRelWorld((X & 0xfffff) << 44L | (Z & cylSize.ringSizeMask) << 24 | (Y & 0xfff) << 12 | (x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf), cylSize)
  def apply(x: Int, y: Int, z: Int, cylSize: CylinderSize): BlockRelWorld = BlockRelWorld(x >> 4, y >> 4, z >> 4, x & 15, y & 15, z & 15, cylSize)
}
case class BlockRelWorld(value: Long, cylSize: CylinderSize) { // XXXXXZZZZZYYYxyz
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
}
case class ChunkRelWorld(value: Long, cylSize: CylinderSize) { // XXXXXZZZZZYYY
  def getChunkRelColumn = ChunkRelColumn((value & 0xfff).toInt, cylSize)
  def getColumnRelWorld = ColumnRelWorld(value >>> 12, cylSize)

  def X: Int = (value >> 20).toInt >> 12
  def Z: Int = value.toInt >> 12
  def Y: Int = (value << 20).toInt >> 20
}

object ColumnRelWorld {
  def apply(X: Long, Z: Int, cylSize: CylinderSize): ColumnRelWorld = ColumnRelWorld((X & 0xfffff) << 20 | (Z & cylSize.ringSizeMask), cylSize)
}
case class ColumnRelWorld(value: Long, cylSize: CylinderSize) { // XXXXXZZZZZ
  def X: Int = (value >> 8).toInt >> 12
  def Z: Int = (value << 12).toInt >> 12

  def distSq(origin: Vector2d): Double = {
    val dx = this.X - origin.x + 0.5
    val dz1 = math.abs(this.Z - origin.y + 0.5 + dx * 0.5f)
    val dz2 = math.abs(dz1 - cylSize.ringSize)
    val dz = math.min(dz1, dz2)
    dx * dx + dz * dz
  }
}
