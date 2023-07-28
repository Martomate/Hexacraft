package hexacraft.world.coord.integer

import hexacraft.math.Int12

object BlockRelColumn {
  def apply(Y: Int, x: Int, y: Int, z: Int): BlockRelColumn =
    BlockRelColumn((Y & 0xfff) << 12 | ((x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf)))

  def apply(block: BlockRelChunk, chunk: Int12): BlockRelColumn =
    BlockRelColumn(chunk.repr.toInt << 12 | block.value)
}

case class BlockRelColumn(value: Int) extends AnyVal { // YYYxyz
  def getBlockRelChunk: BlockRelChunk = BlockRelChunk(value & 0xfff)

  def Y: Int12 = Int12.truncate(value >> 12)
  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte
  def y: Int = Y.toInt * 16 + cy
}
