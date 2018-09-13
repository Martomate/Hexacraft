package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize

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