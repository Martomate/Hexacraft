package com.martomate.hexacraft.world.coord

import scala.collection.Seq

object NeighborOffsets {
  val all: Seq[Offset] = Array(
    Offset( 0, 1, 0),
    Offset( 0,-1, 0),
    Offset( 1, 0, 0),
    Offset( 0, 0, 1),
    Offset(-1, 0, 1),
    Offset(-1, 0, 0),
    Offset( 0, 0,-1),
    Offset( 1, 0,-1))

  def apply(side: Int): Offset = all(side)

  val indices: Range = all.indices
}

case class Offset(dx: Int, dy: Int, dz: Int)
