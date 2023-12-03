package hexacraft.world.coord.integer

import hexacraft.math.bits.{Int12, Int20}
import hexacraft.world.CylinderSize

object BlockRelWorld {
  def apply(X: Long, Y: Int, Z: Long, x: Int, y: Int, z: Int)(using cylSize: CylinderSize): BlockRelWorld =
    BlockRelWorld(
      (X & 0xfffff) << 44L | (Z & cylSize.ringSizeMask) << 24 | (Y & 0xfff) << 12 | (x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf)
    )

  def apply(x: Int, y: Int, z: Int)(using CylinderSize): BlockRelWorld =
    BlockRelWorld(x >> 4, y >> 4, z >> 4, x & 15, y & 15, z & 15)

  def fromChunk(block: BlockRelChunk, chunk: ChunkRelWorld): BlockRelWorld =
    BlockRelWorld(chunk.value << 12 | block.value)

  def apply(i: Int, j: Int, k: Int, chunk: ChunkRelWorld)(using CylinderSize): BlockRelWorld =
    BlockRelWorld(chunk.X.toInt * 16 + i, chunk.Y.toInt * 16 + j, chunk.Z.toInt * 16 + k)
}

case class BlockRelWorld(value: Long) extends AnyVal { // XXXXXZZZZZYYYxyz
  def getBlockRelChunk: BlockRelChunk = BlockRelChunk((value & 0xfff).toInt)
  def getChunkRelWorld: ChunkRelWorld = ChunkRelWorld(value >>> 12)
  def getColumnRelWorld: ColumnRelWorld = ColumnRelWorld(value >>> 24)

  def offset(t: Offset)(using CylinderSize): BlockRelWorld = offset(t.dx, t.dy, t.dz)
  def offset(xx: Int, yy: Int, zz: Int)(using CylinderSize): BlockRelWorld = BlockRelWorld(x + xx, y + yy, z + zz)

  def X: Int20 = Int20.truncate(value >> 44)
  def Z: Int20 = Int20.truncate(value >> 24)
  def Y: Int12 = Int12.truncate(value >>> 12)
  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte
  def x: Int = X.toInt << 4 | cx
  def y: Int = Y.toInt << 4 | cy
  def z: Int = Z.toInt << 4 | cz

  override def toString = s"($x, $y, $z)"
}
