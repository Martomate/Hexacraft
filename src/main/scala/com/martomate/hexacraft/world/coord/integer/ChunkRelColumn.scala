package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize

object ChunkRelColumn {
  def create(Y: Int)(implicit cylSize: CylinderSize): ChunkRelColumn = new ChunkRelColumn(Y & 0xfff)
}

case class ChunkRelColumn(value: Int) extends AnyVal { // YYY
  def Y: Int = value << 20 >> 20
}
