package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.state.BlockState

object BlockRelChunk {
  def apply(x: Int, y: Int, z: Int)(implicit cylSize: CylinderSize): BlockRelChunk = BlockRelChunk((x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf))
}

case class BlockRelChunk(private val _value: Int)(implicit val cylSize: CylinderSize) extends AbstractIntegerCoords(_value) { // xyz
  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte

  def offset(x: Int, y: Int, z: Int): BlockRelChunk = BlockRelChunk(cx + x, cy + y, cz + z)

  def onChunkEdge(side: Int): Boolean = {
    val off = BlockState.neighborOffsets(side)
    val xx = cx + off._1
    val yy = cx + off._2
    val zz = cx + off._3
    (xx & ~15 | yy & ~15 | zz & ~15) != 0
  }

  def neighbor(side: Int): BlockRelChunk = {
    val off = BlockState.neighborOffsets(side)
    val xx = cx + off._1
    val yy = cx + off._2
    val zz = cx + off._3
    BlockRelChunk(xx, yy, zz)
  }

  override def toString: String = s"($cx, $cy, $cz)"
}