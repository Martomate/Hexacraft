package com.martomate.hexacraft.block

object BlockAir extends Block(0, "air", "Air") {
  val State: BlockState = BlockState(this)

  override def canBeRendered: Boolean = false
  override def isTransparent(blockState: BlockState, side: Int): Boolean = true
}
