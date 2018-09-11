package com.martomate.hexacraft.block

object BlockAir extends Block(0, "air", "Air") {
  val State: BlockState = BlockState(this)

  override def canBeRendered: Boolean = false
  override def isTransparent(metadata: Byte, side: Int): Boolean = true
}
