package com.martomate.hexacraft.world.coord.integer

object BlockRelColumn {
  def apply(Y: Int, x: Int, y: Int, z: Int): BlockRelColumn = BlockRelColumn(
    (Y & 0xfff) << 12 | ((x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf))
  )
  def apply(block: BlockRelChunk, chunk: ChunkRelColumn): BlockRelColumn = BlockRelColumn(
    chunk.value << 12 | block.value
  )
}

case class BlockRelColumn(value: Int) extends AnyVal { // YYYxyz
  def getBlockRelChunk: BlockRelChunk = BlockRelChunk(value & 0xfff)
  def getChunkRelColumn: ChunkRelColumn = ChunkRelColumn(value >> 12 & 0xfff)

  def Y: Int = value << 8 >> 20
  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte
  def y: Int = Y * 16 + cy
}
