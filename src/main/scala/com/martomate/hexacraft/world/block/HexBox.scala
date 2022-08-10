package com.martomate.hexacraft.world.block

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.fp.CylCoords

/** radius is the big radius of the hexagon */
class HexBox(val radius: Float, val bottom: Float, val top: Float) {
  val smallRadius: Double = radius * CylinderSize.y60

  def vertices: IndexedSeq[CylCoords] = {
    // val ints = Seq(1, 2, 0, 3, 5, 4)

    for {
      s <- 0 to 1
      i <- 0 until 6
    } yield {
      val v = i * Math.PI / 3
      val x = Math.cos(v).toFloat
      val z = Math.sin(v).toFloat
      CylCoords(x * radius, (1 - s) * (top - bottom) + bottom, z * radius, fixZ = false)(null)
    }
  }
}
