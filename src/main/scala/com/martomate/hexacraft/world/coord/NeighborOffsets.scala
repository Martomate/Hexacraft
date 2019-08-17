package com.martomate.hexacraft.world.coord

import scala.collection.Seq

object NeighborOffsets {
  val all: Seq[(Int, Int, Int)] = IndexedSeq(
    ( 0, 1, 0),
    ( 0,-1, 0),
    ( 1, 0, 0),
    ( 0, 0, 1),
    (-1, 0, 1),
    (-1, 0, 0),
    ( 0, 0,-1),
    ( 1, 0,-1))

  def apply(side: Int): (Int, Int, Int) = all(side)

  def indices: Range = all.indices
}
