package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.{NeighborOffsets, Offset}

object BlockRelChunk {
  def apply(x: Int, y: Int, z: Int)(implicit cylSize: CylinderSize): BlockRelChunk = BlockRelChunk((x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf))
}

case class BlockRelChunk private (value: Int)(implicit val cylSize: CylinderSize) { // xyz
  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte

  def offset(off: Offset): BlockRelChunk = offset(off.dx, off.dy, off.dz)
  def offset(x: Int, y: Int, z: Int): BlockRelChunk = BlockRelChunk(cx + x, cy + y, cz + z)

  def onChunkEdge(side: Int): Boolean = {
    val off = NeighborOffsets(side)
    val xx = cx + off.dx
    val yy = cy + off.dy
    val zz = cz + off.dz
    (xx & ~15 | yy & ~15 | zz & ~15) != 0
  }

  def neighbor(side: Int): BlockRelChunk = {
    val off = NeighborOffsets(side)
    val xx = cx + off.dx
    val yy = cy + off.dy
    val zz = cz + off.dz
    BlockRelChunk(xx, yy, zz)
  }

  override def toString: String = s"($cx, $cy, $cz)"
}
