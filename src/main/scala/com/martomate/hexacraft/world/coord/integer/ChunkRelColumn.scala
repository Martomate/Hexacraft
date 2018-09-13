package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize

case class ChunkRelColumn(private val _value: Int, cylSize: CylinderSize) extends AbstractIntegerCoords(_value & 0xfff) { // YYY
  def Y: Int = value << 20 >> 20
}
