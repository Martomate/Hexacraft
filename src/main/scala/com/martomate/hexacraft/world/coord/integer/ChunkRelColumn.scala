package com.martomate.hexacraft.world.coord.integer

import com.martomate.hexacraft.util.CylinderSize

object ChunkRelColumn {
  def apply(Y: Int)(implicit cylSize: CylinderSize): ChunkRelColumn = new ChunkRelColumn(Y & 0xfff)
}

case class ChunkRelColumn private (value: Int)(implicit val cylSize: CylinderSize) { // YYY
  def Y: Int = value << 20 >> 20
}
