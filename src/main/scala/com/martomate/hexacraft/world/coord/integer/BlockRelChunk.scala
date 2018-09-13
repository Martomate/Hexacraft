package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize

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