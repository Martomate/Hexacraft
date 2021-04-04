package com.martomate.hexacraft.world.coord.integer

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

  /** @param side the side of the origin block (0: top, 1: bottom, 2-7: sides)
    * @return The offset of the neighboring block on the given side
    */
  def apply(side: Int): Offset = all(side)

  val indices: Range = all.indices
}
