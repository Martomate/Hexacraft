package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize

object BlockRelColumn {
  def apply(Y: Int, x: Int, y: Int, z: Int)(implicit cylSize: CylinderSize): BlockRelColumn = BlockRelColumn((Y & 0xfff) << 12 | ((x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf)))
  def apply(block: BlockRelChunk, chunk: ChunkRelColumn): BlockRelColumn = BlockRelColumn(chunk.value << 12 | block.value)(chunk.cylSize)
}

case class BlockRelColumn(private val _value: Int)(implicit val cylSize: CylinderSize) extends AbstractIntegerCoords(_value) { // YYYxyz
  def getBlockRelChunk = BlockRelChunk(value & 0xfff)
  def getChunkRelColumn = ChunkRelColumn(value >> 12 & 0xfff)

  def Y: Int = value << 8 >> 20
  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte
  def y: Int = Y * 16 + cy
}