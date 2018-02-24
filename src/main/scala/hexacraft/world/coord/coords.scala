package hexacraft.world.coord

import hexacraft.world.storage.World
import org.joml.Vector2d

object BlockRelChunk {
  def apply(x: Int, y: Int, z: Int, world: World): BlockRelChunk = BlockRelChunk((x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf), world)
}
case class BlockRelChunk(value: Int, world: World) { // xyz

  def withChunk(chunk: ChunkRelColumn) = BlockRelColumn(chunk.value << 12 | value, world)
  def withChunk(chunk: ChunkRelWorld) = BlockRelWorld(chunk.value << 12 | value, world)

  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte
}

object BlockRelColumn {
  def apply(Y: Int, x: Int, y: Int, z: Int, world: World): BlockRelColumn = BlockRelColumn((Y & 0xfff) << 12 | ((x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf)), world)
}
case class BlockRelColumn(value: Int, world: World) { // YYYxyz
  def withColumn(column: ColumnRelWorld) = BlockRelWorld(column.value << 24L | value, world)

  def getBlockRelChunk = BlockRelChunk(value & 0xfff, world)
  def getChunkRelColumn = ChunkRelColumn(value >> 12 & 0xfff, world)

  def Y: Int = value << 8 >> 20
  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte
}

case class ChunkRelColumn(value: Int, world: World) { // YYY
  def withColumn(column: ColumnRelWorld) = ChunkRelWorld(column.value << 12L | value, world)

  def Y: Int = value << 20 >> 20
}

object BlockRelWorld {
  def apply(X: Long, Y: Int, Z: Long, x: Int, y: Int, z: Int, world: World): BlockRelWorld =
    BlockRelWorld((X & 0xfffff) << 44L | (Z & world.ringSizeMask) << 24 | (Y & 0xfff) << 12 | (x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf), world)
  def apply(x: Int, y: Int, z: Int, world: World): BlockRelWorld = BlockRelWorld(x >> 4, y >> 4, z >> 4, x & 15, y & 15, z & 15, world)
}
case class BlockRelWorld(value: Long, world: World) { // XXXXXZZZZZYYYxyz
  def getBlockRelChunk = BlockRelChunk((value & 0xfff).toInt, world)
  def getBlockRelColumn = BlockRelColumn((value & 0xffffff).toInt, world)
  def getChunkRelColumn = ChunkRelColumn((value >>> 12 & 0xfff).toInt, world)
  def getChunkRelWorld = ChunkRelWorld(value >>> 12, world)
  def getColumnRelWorld = ColumnRelWorld(value >>> 24, world)

  def offset(xx: Int, yy: Int, zz: Int) = BlockRelWorld(x + xx, y + yy, z + zz, world)

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
  def apply(X: Long, Y: Int, Z: Int, world: World): ChunkRelWorld = ChunkRelWorld((X & 0xfffff) << 32 | (Z & world.ringSizeMask) << 12 | (Y & 0xfff), world)
}
case class ChunkRelWorld(value: Long, world: World) { // XXXXXZZZZZYYY
  def getChunkRelColumn = ChunkRelColumn((value & 0xfff).toInt, world)
  def getColumnRelWorld = ColumnRelWorld(value >>> 12, world)

  def X: Int = (value >> 20).toInt >> 12
  def Z: Int = value.toInt >> 12
  def Y: Int = (value << 20).toInt >> 20
}

object ColumnRelWorld {
  def apply(X: Long, Z: Int, world: World): ColumnRelWorld = ColumnRelWorld((X & 0xfffff) << 20 | (Z & world.ringSizeMask), world)
}
case class ColumnRelWorld(value: Long, world: World) { // XXXXXZZZZZ
  def X: Int = (value >> 8).toInt >> 12
  def Z: Int = (value << 12).toInt >> 12

  def distSq(origin: Vector2d): Double = {
    val dx = this.X - origin.x + 0.5
    val dz1 = math.abs(this.Z - origin.y + 0.5 + dx * 0.5f)
    val dz2 = math.abs(dz1 - world.ringSize)
    val dz = math.min(dz1, dz2)
    dx * dx + dz * dz
  }
}
